package com.filesharing.service.impl;

import com.filesharing.dto.FolderCreateRequest;
import com.filesharing.dto.FolderResponse;
import com.filesharing.dto.response.FolderSimpleResponse;
import com.filesharing.entity.Folder;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FolderRepository;
import com.filesharing.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件夹服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FolderServiceImpl implements FolderService {
    
    private final FolderRepository folderRepository;
    
    @Override
    public FolderResponse createFolder(FolderCreateRequest request, User owner) {
        if (owner == null) {
            throw new BusinessException("用户信息不能为空");
        }
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException("文件夹名称不能为空");
        }

        String folderName = request.getName().trim();
        Folder parent = null;
        if (request.getParentId() != null) {
            parent = getFolderEntityById(request.getParentId());
            if (!parent.getOwner().getId().equals(owner.getId())) {
                throw new BusinessException("无权在该目录下创建文件夹");
            }
        }

        if (folderRepository.findByOwnerAndNameAndParent(owner, folderName, parent).isPresent()) {
            throw new BusinessException("同级目录下已存在同名文件夹");
        }

        Folder folder = new Folder();
        folder.setName(folderName);
        folder.setDescription(request.getDescription());
        folder.setParent(parent);
        folder.setOwner(owner);
        folder.setIsPublic(request.getIsPublic() != null && request.getIsPublic());

        String parentPath = parent == null ? "" : (parent.getFolderPath() == null ? "" : parent.getFolderPath());
        String separator = parentPath.isEmpty() || parentPath.endsWith("/") ? "" : "/";
        folder.setFolderPath(parentPath + separator + "/" + folderName.replace("//", "/"));

        Folder saved = folderRepository.save(folder);
        return convertToFolderResponse(saved, false);
    }
    
    @Override
    public FolderResponse getFolderById(Long folderId) {
        return convertToFolderResponse(getFolderEntityById(folderId), true);
    }
    
    @Override
    public List<FolderResponse> getRootFolders(User owner) {
        return folderRepository.findRootFoldersByOwner(owner).stream()
                .sorted(Comparator.comparing(Folder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(folder -> convertToFolderResponse(folder, true))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FolderSimpleResponse> getSubFolders(Long folderId, User currentUser) {
        Folder parentFolder = getFolderEntityById(folderId);
        if (parentFolder.getChildren() != null) {
            return parentFolder.getChildren().stream()
                    .map(this::convertToFolderSimpleResponse)
                    .collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
    
    @Override
    public List<FolderResponse> getFolderTree(User owner) {
        List<Folder> allFolders = folderRepository.findByOwner(owner);
        return allFolders.stream()
                .sorted(Comparator.comparing(Folder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(folder -> convertToFolderResponse(folder, false))
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteFolder(Long folderId, User currentUser) {
        Folder folder = getFolderEntityById(folderId);
        assertFolderOwner(folder, currentUser);

        if (folder.getChildren() != null && !folder.getChildren().isEmpty()) {
            throw new BusinessException("请先删除子文件夹");
        }
        if (folder.getFiles() != null && !folder.getFiles().isEmpty()) {
            throw new BusinessException("请先删除文件夹中的文件");
        }

        folderRepository.delete(folder);
    }
    
    @Override
    public FolderResponse renameFolder(Long folderId, String newName, User currentUser) {
        Folder folder = getFolderEntityById(folderId);
        assertFolderOwner(folder, currentUser);

        if (newName == null || newName.trim().isEmpty()) {
            throw new BusinessException("文件夹名称不能为空");
        }
        String normalizedName = newName.trim();

        Folder parent = folder.getParent();
        folderRepository.findByOwnerAndNameAndParent(currentUser, normalizedName, parent)
                .filter(existing -> !existing.getId().equals(folder.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("同级目录下已存在同名文件夹");
                });

        folder.setName(normalizedName);
        String basePath = parent == null ? "" : (parent.getFolderPath() == null ? "" : parent.getFolderPath());
        String separator = basePath.isEmpty() || basePath.endsWith("/") ? "" : "/";
        String newPath = (basePath + separator + "/" + normalizedName).replace("//", "/");
        updateFolderPathRecursively(folder, newPath);

        Folder saved = folderRepository.save(folder);
        return convertToFolderResponse(saved, true);
    }
    
    @Override
    public FolderResponse moveFolder(Long folderId, Long targetParentId, User currentUser) {
        Folder folder = getFolderEntityById(folderId);
        assertFolderOwner(folder, currentUser);

        Folder targetParent = null;
        if (targetParentId != null) {
            targetParent = getFolderEntityById(targetParentId);
            assertFolderOwner(targetParent, currentUser);
            if (isDescendant(folder, targetParent)) {
                throw new BusinessException("不能将文件夹移动到其子目录");
            }
        }

        folderRepository.findByOwnerAndNameAndParent(currentUser, folder.getName(), targetParent)
                .filter(existing -> !existing.getId().equals(folder.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("目标目录已存在同名文件夹");
                });

        folder.setParent(targetParent);
        String basePath = targetParent == null ? "" : (targetParent.getFolderPath() == null ? "" : targetParent.getFolderPath());
        String separator = basePath.isEmpty() || basePath.endsWith("/") ? "" : "/";
        String newPath = (basePath + separator + "/" + folder.getName()).replace("//", "/");
        updateFolderPathRecursively(folder, newPath);

        Folder saved = folderRepository.save(folder);
        return convertToFolderResponse(saved, true);
    }
    
    @Override
    public FolderResponse setFolderPublic(Long folderId, Boolean isPublic, User currentUser) {
        Folder folder = getFolderEntityById(folderId);
        assertFolderOwner(folder, currentUser);
        folder.setIsPublic(isPublic != null && isPublic);
        Folder saved = folderRepository.save(folder);
        return convertToFolderResponse(saved, true);
    }
    
    @Override
    public String getFolderPath(Long folderId) {
        return getFolderEntityById(folderId).getFolderPath();
    }
    
    @Override
    public Folder getFolderEntityById(Long folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException("文件夹不存在"));
    }
    
    @Override
    public boolean hasFolderPermission(Long folderId, User user) {
        Folder folder = getFolderEntityById(folderId);
        return folder.getOwner().getId().equals(user.getId()) || Boolean.TRUE.equals(folder.getIsPublic());
    }
    
    // 移动端专用方法实现
    
    @Override
    public List<FolderSimpleResponse> getUserFolderTree(User user) {
        List<Folder> folders = folderRepository.findByOwner(user);
        return folders.stream()
                .map(this::convertToFolderSimpleResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FolderSimpleResponse> getFolderBreadcrumb(Long folderId, User user) {
        Folder folder = getFolderEntityById(folderId);
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw new BusinessException("无权访问该文件夹");
        }

        List<FolderSimpleResponse> breadcrumb = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            breadcrumb.add(convertToFolderSimpleResponse(current));
            current = current.getParent();
        }
        Collections.reverse(breadcrumb);
        return breadcrumb;
    }
    
    @Override
    public List<FolderSimpleResponse> getQuickAccessFolders(User user) {
        return folderRepository.findByOwner(user).stream()
                .sorted(Comparator.comparing(Folder::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::convertToFolderSimpleResponse)
                .collect(Collectors.toList());
    }

    private void assertFolderOwner(Folder folder, User user) {
        if (folder == null || user == null || !folder.getOwner().getId().equals(user.getId())) {
            throw new BusinessException("无权操作该文件夹");
        }
    }

    private boolean isDescendant(Folder folder, Folder possibleChild) {
        Folder current = possibleChild;
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void updateFolderPathRecursively(Folder folder, String newPath) {
        folder.setFolderPath(newPath);
        if (folder.getChildren() == null) {
            return;
        }
        for (Folder child : folder.getChildren()) {
            String childPath = (newPath + "/" + child.getName()).replace("//", "/");
            updateFolderPathRecursively(child, childPath);
        }
    }

    private FolderResponse convertToFolderResponse(Folder folder, boolean includeChildren) {
        List<FolderResponse> children = null;
        if (includeChildren && folder.getChildren() != null) {
            children = folder.getChildren().stream()
                    .map(child -> convertToFolderResponse(child, false))
                    .collect(Collectors.toList());
        }

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .parentName(folder.getParent() != null ? folder.getParent().getName() : null)
                .ownerId(folder.getOwner() != null ? folder.getOwner().getId() : null)
                .ownerName(folder.getOwner() != null ? folder.getOwner().getUsername() : null)
                .isPublic(folder.getIsPublic())
                .folderPath(folder.getFolderPath())
                .fileCount(folder.getFiles() != null ? folder.getFiles().size() : 0)
                .subFolderCount(folder.getChildren() != null ? folder.getChildren().size() : 0)
                .children(children)
                .createdAt(folder.getCreatedAt() != null ? folder.getCreatedAt().toString() : null)
                .updatedAt(folder.getUpdatedAt() != null ? folder.getUpdatedAt().toString() : null)
                .build();
    }
    
    // 转换方法
    private FolderSimpleResponse convertToFolderSimpleResponse(Folder folder) {
        return FolderSimpleResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .icon("folder") // 简化图标
                .fileCount(folder.getFiles() != null ? (long) folder.getFiles().size() : 0L)
                .subFolderCount(folder.getChildren() != null ? (long) folder.getChildren().size() : 0L)
                .isQuickAccess(false)
                .createdAt(folder.getCreatedAt())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .fullPath(folder.getFolderPath())
                .build();
    }
}