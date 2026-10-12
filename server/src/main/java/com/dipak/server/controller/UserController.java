package com.dipak.server.controller;

import com.dipak.server.dto.UserDto;
import com.dipak.server.dto.UserKeyDto;
import com.dipak.server.security.SecurityUtils;
import com.dipak.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Users", description = "Profile, search, key management, block")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<UserDto> getMe() {
        return ResponseEntity.ok(userService.getMe(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by name or email prefix (2-10 chars)")
    public ResponseEntity<List<UserDto>> search(
            @RequestParam @Size(min = 2, max = 10, message = "Query must be 2-10 characters") String q) {
        return ResponseEntity.ok(userService.search(q, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/me/keys")
    @Operation(summary = "Upload or update your public key bundle")
    public ResponseEntity<UserKeyDto> uploadKeys(@Valid @RequestBody UserKeyDto dto) {
        return ResponseEntity.ok(userService.uploadKeys(SecurityUtils.getCurrentUserId(), dto));
    }

    @GetMapping("/{id}/keys")
    @Operation(summary = "Get another user's public key (needed for E2E encryption)")
    public ResponseEntity<UserKeyDto> getKeys(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getKeys(id));
    }
}
