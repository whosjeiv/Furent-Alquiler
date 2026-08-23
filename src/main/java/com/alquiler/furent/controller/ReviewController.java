package com.alquiler.furent.controller;

import com.alquiler.furent.model.Review;
import com.alquiler.furent.model.User;
import com.alquiler.furent.service.ReviewService;
import com.alquiler.furent.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @PostMapping("/producto/reseña")
    public String addReview(@RequestParam String productId,
            @RequestParam int rating,
            @RequestParam String comment,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        if (auth == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión para dejar una reseña.");
            return "redirect:/producto/" + productId;
        }

        Optional<User> optUser = userService.findByEmail(auth.getName());
        if (!optUser.isPresent()) {
            return "redirect:/producto/" + productId;
        }

        User user = optUser.get();
        String userName = user.getNombre()
                + (user.getApellido() != null && !user.getApellido().isEmpty() ? " " + user.getApellido() : "");

        Review review = new Review(productId, user.getId(), userName, rating, comment);
        reviewService.saveReview(review);

        redirectAttributes.addFlashAttribute("successReview", "¡Gracias! Tu reseña ha sido publicada exitosamente.");
        return "redirect:/producto/" + productId + "#reviews-section";
    }
}
