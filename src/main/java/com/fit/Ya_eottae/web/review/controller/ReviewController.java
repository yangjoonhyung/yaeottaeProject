package com.fit.Ya_eottae.web.review.controller;

import com.fit.Ya_eottae.SessionConst;
import com.fit.Ya_eottae.domain.comment.Comment;
import com.fit.Ya_eottae.domain.member.Member;
import com.fit.Ya_eottae.domain.review.Review;
import com.fit.Ya_eottae.domain.review.ReviewUpdateDto;
import com.fit.Ya_eottae.repository.commentrepository.CommentRepository;
import com.fit.Ya_eottae.repository.memberrepository.MemberRepository;
import com.fit.Ya_eottae.repository.reviewrepository.ReviewRepository;
import com.fit.Ya_eottae.web.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;
    private final ReviewService reviewService;

    @GetMapping("/{reviewId}")
    public String review(@PathVariable long reviewId, Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Review review = reviewRepository.findById(reviewId).get();
        model.addAttribute("review", review);
        List<Comment> findCommentList = commentRepository.findByReviewId(reviewId);
        model.addAttribute("comment", findCommentList);

        if (session == null) {
            return "review/review";
        }

        String memberId = (String) session.getAttribute(SessionConst.SESSION_ID);


        List<Review> allReview = reviewRepository.findAllReview();
        String reviewTrustScore = reviewService.calculateBayesianAverage(review, allReview);
        review.setTrustScore(reviewTrustScore);



        if (review.getMember().getMemberId().equals(memberId)) {
            return "review/my-review";
        }

        return "review/review";
    }

    @GetMapping("/{restaurantId}/review-save")
    public String reviewSaveForm(@PathVariable long restaurantId) {
        return "review/reviewSave";
    }

    @PostMapping("/{restaurantId}/review-save")
    public String reviewSave(@PathVariable long restaurantId, @ModelAttribute Review review, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String memberId = (String) session.getAttribute(SessionConst.SESSION_ID);
        Member findMember = memberRepository.findByMemberId(memberId);

        Review saveReview = new Review(review.getReviewName(), review.getReviewDetail(), review.getReviewPoint(),
                review.getIsAdvertisement(), restaurantId);

        saveReview.setMember(findMember);

        reviewRepository.save(saveReview);
        return "redirect:/restaurant/{restaurantId}";
    }

    @GetMapping("/{reviewId}/review-update")
    public String reviewUpdateForm(@PathVariable long reviewId, Model model) {
        Review findReview = reviewRepository.findById(reviewId).get();
        model.addAttribute("review", findReview);
        return "/review/update-review";
    }

    @PostMapping("/{reviewId}/review-update")
    public String reviewUpdate(@PathVariable long reviewId, @ModelAttribute("updateReview") ReviewUpdateDto updateParam) {
        reviewRepository.updateReview(reviewId, updateParam);
        return "redirect:/review/{reviewId}";
    }

    @PostMapping("/{reviewId}/plus")
    public String plusTrustPoint(@PathVariable long reviewId) {
        log.info("Plus={}", reviewId);
        reviewRepository.plusTrustPoint(reviewId);
        return "redirect:/review/{reviewId}";
    }

    @PostMapping("/{reviewId}/minus")
    public String plusNoTrustPoint(@PathVariable long reviewId) {
        log.info("Minus={}", reviewId);
        reviewRepository.plusNoTrustPoint(reviewId);
        return "redirect:/review/{reviewId}";
    }

    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable long reviewId) {
        reviewRepository.deleteReview(reviewId);
        return "redirect:/";
    }

    @PostMapping("/{reviewId}/comment-save")
    public String commentSave(@PathVariable long reviewId, @ModelAttribute Comment comment, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        String memberId = (String) session.getAttribute(SessionConst.SESSION_ID);
        Review findReview = reviewRepository.findById(reviewId).get();

        Comment saveComment = new Comment(comment.getComment());
        saveComment.setReview(findReview);
        saveComment.setMemberId(memberId);
        commentRepository.save(saveComment);
        return "redirect:/review/{reviewId}";
    }

    @PostMapping("{reviewId}/{commentId}/delete-comment")
    public String deleteComment(@PathVariable long reviewId, @PathVariable long commentId , RedirectAttributes redirectAttributes) {
        List<Comment> findCommentList = commentRepository.findByReviewId(reviewId);
        Comment deleteComment = findCommentList.stream().filter(comment -> comment.getCommentId() == commentId).findAny().orElse(null);
        commentRepository.deleteComment(deleteComment.getCommentId());
        return "redirect:/review/{reviewId}";
    }

}