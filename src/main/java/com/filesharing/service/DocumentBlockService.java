package com.filesharing.service;

import com.filesharing.entity.User;

import java.util.List;

public interface DocumentBlockService {

    void initDocumentBlocks(Long documentId, int blockCount, String initialContent);

    void ensureDocumentHasBlocks(Long documentId, String initialContent);

    List<BlockView> getDocumentBlocks(Long documentId, User currentUser);

    BlockView lockBlock(Long documentId, Long blockId, User currentUser);

    BlockView unlockBlock(Long documentId, Long blockId, User currentUser);

    BlockView updateBlock(Long documentId, Long blockId, String content, Integer expectedVersion, User currentUser);

    BlockView createBlockAfter(Long documentId, Long afterBlockId, User currentUser);

    void unlockBlocksByUser(Long documentId, Long userId);

    class BlockView {
        private Long id;
        private Long documentId;
        private String content;
        private Integer orderIndex;
        private Long lockedByUserId;
        private String lockedUserName;
        private Long lockedAt;
        private Integer version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }

        public Long getLockedByUserId() {
            return lockedByUserId;
        }

        public void setLockedByUserId(Long lockedByUserId) {
            this.lockedByUserId = lockedByUserId;
        }

        public String getLockedUserName() {
            return lockedUserName;
        }

        public void setLockedUserName(String lockedUserName) {
            this.lockedUserName = lockedUserName;
        }

        public Long getLockedAt() {
            return lockedAt;
        }

        public void setLockedAt(Long lockedAt) {
            this.lockedAt = lockedAt;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }
}