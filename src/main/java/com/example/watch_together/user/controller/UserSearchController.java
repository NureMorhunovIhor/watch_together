package com.example.watch_together.user.controller;

import com.example.watch_together.user.dto.UpdateProfileRequest;
import com.example.watch_together.user.dto.UserProfileResponse;
import com.example.watch_together.user.dto.UserSearchResponse;
import com.example.watch_together.user.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserSearchService userSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String query,
                                                                Principal principal) {
        return ResponseEntity.ok(userSearchService.searchUsers(query, principal));
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMe(
            @RequestBody UpdateProfileRequest request,
            Principal principal
    ) {
        return userSearchService.updateCurrentUser(request, principal);
    }

}