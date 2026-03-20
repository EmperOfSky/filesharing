package com.filesharing.service.impl;

import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.DocumentBlock;
import com.filesharing.entity.ProjectMember;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CollaborativeDocumentRepository;
import com.filesharing.repository.DocumentBlockRepository;
import com.filesharing.repository.ProjectMemberRepository;
import com.filesharing.service.DocumentBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentBlockServiceImpl implements DocumentBlockService {

    private final CollaborativeDocumentRepository documentRepository;
    private final DocumentBlockRepository blockRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void initDocumentBlocks(Long documentId, int blockCount, String initialContent) {
        CollaborativeDocument document = findDocument(documentId);
        long existingCount = blockRepository.countByDocumentId(documentId);
        if (existingCount > 0) {
            return;
        }

        int safeBlockCount = Math.max(1, blockCount);
        String firstBlockContent = initialContent == null ? "" : initialContent;

        List<DocumentBlock> blocks = new ArrayList<>();
        for (int i = 0; i < safeBlockCount; i++) {
            DocumentBlock block = new DocumentBlock();
            block.setDocument(document);
            block.setOrderIndex(i);
            block.setContent(i == 0 ? firstBlockContent : "");
            block.setLockedBy(null);
            block.setLockedAt(null);
            blocks.add(block);
        }
        blockRepository.saveAll(blocks);
        syncDocumentContentFromBlocks(document, null);
    }

    public void ensureDocumentHasBlocks(Long documentId, String initialContent) {
        CollaborativeDocument document = findDocument(documentId);
        long existingCount = blockRepository.countByDocumentId(documentId);
        if (existingCount > 0) {
            return; // Blocks already exist
        }

        // Initialize single block for documents without blocks
        DocumentBlock block = new DocumentBlock();
        block.setDocument(document);
        block.setOrderIndex(0);
        block.setContent(initialContent == null ? "" : initialContent);
        block.setLockedBy(null);
        block.setLockedAt(null);
        blockRepository.save(block);
        syncDocumentContentFromBlocks(document, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockView> getDocumentBlocks(Long documentId, User currentUser) {
        if (!canViewDocument(documentId, currentUser)) {
            throw new BusinessException("无权限查看该文档");
        }
        CollaborativeDocument document = findDocument(documentId);
        List<DocumentBlock> blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(documentId);
        if (blocks.isEmpty()) {
            ensureDocumentHasBlocks(documentId, document.getContent());
            blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(documentId);
        }

        return blocks.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public BlockView lockBlock(Long documentId, Long blockId, User currentUser) {
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限编辑该文档");
        }
        DocumentBlock block = resolveBlockForEditing(documentId, blockId);
        Long targetBlockId = block.getId();
        if (block.getLockedBy() != null && !Objects.equals(block.getLockedBy().getId(), currentUser.getId())) {
            throw new BusinessException("该段落正在被他人编辑");
        }

        int updated = blockRepository.lockBlock(documentId, targetBlockId, currentUser, LocalDateTime.now(), currentUser.getId());
        if (updated == 0) {
            throw new BusinessException("Block is being edited by another user");
        }
        return toView(findBlock(documentId, targetBlockId));
    }

    @Override
    public BlockView unlockBlock(Long documentId, Long blockId, User currentUser) {
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限编辑该文档");
        }
        DocumentBlock block = resolveBlockForEditing(documentId, blockId);
        Long targetBlockId = block.getId();
        if (block.getLockedBy() != null
                && !Objects.equals(block.getLockedBy().getId(), currentUser.getId())
                && !canManageProject(documentId, currentUser)) {
            throw new BusinessException("仅锁定者或管理员可解锁该段");
        }

        if (block.getLockedBy() == null) {
            return toView(block);
        }

        boolean canForceUnlock = canManageProject(documentId, currentUser)
                && !Objects.equals(block.getLockedBy().getId(), currentUser.getId());
        int updated = canForceUnlock
            ? blockRepository.forceUnlockBlock(documentId, targetBlockId)
            : blockRepository.unlockBlock(documentId, targetBlockId, currentUser.getId());
        if (updated == 0) {
            throw new BusinessException("Unlock block failed, please refresh and retry");
        }
        return toView(findBlock(documentId, targetBlockId));
    }

    @Override
    public BlockView updateBlock(
            Long documentId,
            Long blockId,
            String content,
            Integer expectedVersion,
            User currentUser) {
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限编辑该文档");
        }

        DocumentBlock block = resolveBlockForEditing(documentId, blockId);
        if (block.getLockedBy() != null && !Objects.equals(block.getLockedBy().getId(), currentUser.getId())) {
            throw new BusinessException("该段落正在被他人编辑");
        }

        if (expectedVersion != null && block.getVersion() != null && !Objects.equals(block.getVersion(), expectedVersion)) {
            throw new BusinessException("段落版本冲突，请先同步后重试");
        }

        block.setLockedBy(currentUser);
        block.setLockedAt(LocalDateTime.now());
        block.setContent(content == null ? "" : content);
        DocumentBlock saved = blockRepository.save(block);

        CollaborativeDocument document = findDocument(documentId);
        syncDocumentContentFromBlocks(document, currentUser);
        return toView(saved);
    }

    @Override
    public BlockView createBlockAfter(Long documentId, Long afterBlockId, User currentUser) {
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限编辑该文档");
        }

        CollaborativeDocument document = findDocument(documentId);
        DocumentBlock previousBlock = resolvePreviousBlockForCreate(documentId, afterBlockId, document.getContent());

        int targetOrderIndex;
        if (previousBlock == null) {
            targetOrderIndex = 0;
        } else {
            blockRepository.shiftOrderIndexAfter(documentId, previousBlock.getOrderIndex());
            targetOrderIndex = previousBlock.getOrderIndex() + 1;
        }


        DocumentBlock newBlock = new DocumentBlock();
        newBlock.setDocument(document);
        newBlock.setContent("");
        newBlock.setOrderIndex(targetOrderIndex);
        newBlock.setLockedBy(currentUser);
        newBlock.setLockedAt(LocalDateTime.now());
        DocumentBlock saved = blockRepository.save(newBlock);

        syncDocumentContentFromBlocks(document, currentUser);
        return toView(saved);
    }

    @Override
    public void unlockBlocksByUser(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            return;
        }
        blockRepository.clearLocksByDocumentAndUser(documentId, userId);
    }

    private CollaborativeDocument findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存"));
    }

    private DocumentBlock findBlock(Long documentId, Long blockId) {
        return blockRepository.findByIdAndDocumentId(blockId, documentId)
                .orElseThrow(() -> new BusinessException(
                        String.format("文档块不存在: documentId=%s, blockId=%s", documentId, blockId)));
    }

    private DocumentBlock resolveBlockForEditing(Long documentId, Long blockId) {
        if (blockId != null && blockId > 0) {
            return findBlock(documentId, blockId);
        }

        CollaborativeDocument document = findDocument(documentId);
        List<DocumentBlock> blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(documentId);
        if (blocks.isEmpty()) {
            ensureDocumentHasBlocks(documentId, document.getContent());
            blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(documentId);
        }
        if (blocks.isEmpty()) {
            throw new BusinessException("文档暂无可编辑段落，请刷新后重试");
        }

        DocumentBlock fallback = blocks.get(0);
        log.warn("收到非法段落ID，回退到首段落: documentId={}, inputBlockId={}, fallbackBlockId={}",
                documentId,
                blockId,
                fallback.getId());
        return fallback;
    }

    private DocumentBlock resolvePreviousBlockForCreate(Long documentId, Long afterBlockId, String initialContent) {
        List<DocumentBlock> blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(documentId);
        if (blocks.isEmpty()) {
            ensureDocumentHasBlocks(documentId, initialContent);
            blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(documentId);
        }

        if (blocks.isEmpty()) {
            return null;
        }

        if (afterBlockId == null || afterBlockId <= 0) {
            return blocks.get(blocks.size() - 1);
        }

        DocumentBlock target = blockRepository.findByIdAndDocumentId(afterBlockId, documentId).orElse(null);
        if (target != null) {
            return target;
        }

        DocumentBlock fallback = blocks.get(blocks.size() - 1);
        log.warn("CREATE_BLOCK afterBlockId not found, fallback to tail insert: documentId={}, afterBlockId={}, fallbackBlockId={}",
                documentId,
                afterBlockId,
                fallback.getId());
        return fallback;
    }

    private boolean canViewDocument(Long documentId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (currentUser == null || document.getProject() == null) {
            return false;
        }

        if (document.getProject().getOwner() != null
                && Objects.equals(document.getProject().getOwner().getId(), currentUser.getId())) {
            return true;
        }

        return projectMemberRepository.findByProjectAndUser(document.getProject(), currentUser)
                .filter(member -> member.getStatus() == ProjectMember.MemberStatus.ACTIVE
                        && member.getInviteStatus() == ProjectMember.InviteStatus.ACCEPTED)
                .isPresent();
    }

    private boolean canEditDocument(Long documentId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (currentUser == null || document.getProject() == null) {
            return false;
        }

        if (document.getProject().getOwner() != null
                && Objects.equals(document.getProject().getOwner().getId(), currentUser.getId())) {
            return true;
        }

        return projectMemberRepository.findByProjectAndUser(document.getProject(), currentUser)
                .filter(member -> member.getStatus() == ProjectMember.MemberStatus.ACTIVE
                        && member.getInviteStatus() == ProjectMember.InviteStatus.ACCEPTED)
                .map(member -> member.getRole() == ProjectMember.MemberRole.ADMIN
                        || member.getRole() == ProjectMember.MemberRole.MEMBER)
                .orElse(false);
    }

    private boolean canManageProject(Long documentId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (currentUser == null || document.getProject() == null) {
            return false;
        }

        if (document.getProject().getOwner() != null
                && Objects.equals(document.getProject().getOwner().getId(), currentUser.getId())) {
            return true;
        }

        return projectMemberRepository.findByProjectAndUser(document.getProject(), currentUser)
                .filter(member -> member.getStatus() == ProjectMember.MemberStatus.ACTIVE
                        && member.getInviteStatus() == ProjectMember.InviteStatus.ACCEPTED)
                .map(member -> member.getRole() == ProjectMember.MemberRole.ADMIN)
                .orElse(false);
    }

    private void syncDocumentContentFromBlocks(CollaborativeDocument document, User operator) {
        List<DocumentBlock> blocks = blockRepository.findByDocumentIdOrderByOrderIndexAsc(document.getId());
        String merged = blocks.stream()
                .sorted(Comparator.comparing(DocumentBlock::getOrderIndex))
                .map(block -> block.getContent() == null ? "" : block.getContent())
                .collect(Collectors.joining("\n\n"));

        document.setContent(merged);
        document.setVersion((document.getVersion() == null ? 0 : document.getVersion()) + 1);
        document.setLastEditedBy(operator);
        document.setLastEditedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    private BlockView toView(DocumentBlock block) {
        BlockView view = new BlockView();
        view.setId(block.getId());
        view.setDocumentId(block.getDocument() == null ? null : block.getDocument().getId());
        view.setContent(block.getContent() == null ? "" : block.getContent());
        view.setOrderIndex(block.getOrderIndex());
        view.setLockedByUserId(block.getLockedBy() == null ? null : block.getLockedBy().getId());
        view.setLockedUserName(block.getLockedBy() == null ? null : block.getLockedBy().getUsername());
        view.setLockedAt(block.getLockedAt() == null ? null : block.getLockedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        view.setVersion(block.getVersion() == null ? 0 : block.getVersion());
        return view;
    }
}
