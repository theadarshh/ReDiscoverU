package com.rediscoveru.service;

import com.rediscoveru.dto.CommentRequest;
import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final CommentRepository     commentRepo;
    private final CommentLikeRepository likeRepo;
    private final UserRepository        userRepo;
    private final ProgramRepository     programRepo;
    private final NotificationService   notifService;

    // ════════════════════════ COMMENTS ══════════════════════════════

    @Transactional
    public Comment postComment(String email, CommentRequest req) {
        User    user    = resolveUser(email);
        Program program = resolveProgram(req.getProgramId());

        if (req.getCommentText() == null || req.getCommentText().isBlank())
            throw new RuntimeException("Comment text cannot be empty");
        if (req.getCommentText().length() > 2000)
            throw new RuntimeException("Comment too long — max 2000 characters");

        // Validate parent exists if replying
        if (req.getParentId() != null) {
            Comment parent = commentRepo.findById(req.getParentId())
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (parent.isDeleted()) throw new RuntimeException("Cannot reply to a deleted comment");
            // Increment parent's reply count
            parent.setReplyCount(parent.getReplyCount() + 1);
            commentRepo.save(parent);
            // Notify original commenter (if not replying to yourself)
            if (!parent.getUser().getId().equals(user.getId())) {
                notifService.onCommentReply(parent.getUser(), program.getTitle());
            }
        }

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setProgram(program);
        comment.setContentId(req.getContentId());
        comment.setContentType(req.getContentType());
        comment.setCommentText(req.getCommentText().trim());
        comment.setParentId(req.getParentId());
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepo.save(comment);
    }

    @Transactional
    public Comment editComment(String email, Long commentId, String newText) {
        User user = resolveUser(email);
        Comment comment = commentRepo.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUser().getId().equals(user.getId()))
            throw new RuntimeException("You can only edit your own comments");
        if (comment.isDeleted()) throw new RuntimeException("Cannot edit a deleted comment");
        if (newText == null || newText.isBlank()) throw new RuntimeException("Comment text required");
        comment.setCommentText(newText.trim());
        comment.setUpdatedAt(LocalDateTime.now());
        return commentRepo.save(comment);
    }

    @Transactional
    public void deleteComment(String email, Long commentId, boolean isAdmin) {
        User user = resolveUser(email);
        Comment comment = commentRepo.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!isAdmin && !comment.getUser().getId().equals(user.getId()))
            throw new RuntimeException("You can only delete your own comments");
        // Soft delete — keep record for thread integrity
        comment.setDeleted(true);
        comment.setCommentText("[This comment was deleted]");
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepo.save(comment);
    }

    /**
     * Returns top-level comments + their replies for a content item.
     * Annotates each comment with whether the current user liked it.
     */
    public List<Map<String, Object>> getCommentThread(
            String email, Long programId, Long contentId, String contentType) {
        User user = resolveUser(email);
        List<Comment> topLevel = commentRepo.findTopLevelComments(programId, contentId, contentType);
        if (topLevel.isEmpty()) return List.of();

        List<Long> topIds = topLevel.stream().map(Comment::getId).collect(Collectors.toList());
        List<Long> likedIds = likeRepo.findLikedCommentIds(user.getId(), topIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment c : topLevel) {
            Map<String, Object> m = commentToMap(c, likedIds.contains(c.getId()));

            // Fetch replies
            List<Comment> replies = commentRepo.findReplies(c.getId());
            List<Long> replyIds = replies.stream().map(Comment::getId).collect(Collectors.toList());
            List<Long> likedReplyIds = replyIds.isEmpty() ? List.of()
                : likeRepo.findLikedCommentIds(user.getId(), replyIds);

            m.put("replies", replies.stream()
                .map(r -> commentToMap(r, likedReplyIds.contains(r.getId())))
                .collect(Collectors.toList()));
            result.add(m);
        }
        return result;
    }

    // ════════════════════════ LIKES ═════════════════════════════════

    @Transactional
    public Map<String, Object> toggleLike(String email, Long commentId) {
        User user = resolveUser(email);
        Comment comment = commentRepo.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (comment.isDeleted()) throw new RuntimeException("Cannot like a deleted comment");

        Optional<CommentLike> existing = likeRepo.findByUserIdAndCommentId(user.getId(), commentId);
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepo.save(comment);
            return Map.of("liked", false, "likeCount", comment.getLikeCount());
        } else {
            CommentLike like = new CommentLike();
            like.setUser(user);
            like.setComment(comment);
            likeRepo.save(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepo.save(comment);
            return Map.of("liked", true, "likeCount", comment.getLikeCount());
        }
    }

    // ════════════════════════ HELPERS ═══════════════════════════════

    private Map<String, Object> commentToMap(Comment c, boolean liked) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           c.getId());
        m.put("commentText",  c.getCommentText());
        m.put("userName",     c.getUser() != null ? c.getUser().getName() : "Unknown");
        m.put("likeCount",    c.getLikeCount());
        m.put("replyCount",   c.getReplyCount());
        m.put("parentId",     c.getParentId());
        m.put("deleted",      c.isDeleted());
        m.put("likedByMe",    liked);
        m.put("createdAt",    c.getCreatedAt());
        m.put("updatedAt",    c.getUpdatedAt());
        return m;
    }

    private User resolveUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
    private Program resolveProgram(Long id) {
        return programRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Program not found: " + id));
    }
}
