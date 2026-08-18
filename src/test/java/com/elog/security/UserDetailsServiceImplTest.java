package com.elog.security;

import com.elog.entity.Permission;
import com.elog.entity.Role;
import com.elog.entity.User;
import com.elog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_success() {
        Permission perm = Permission.builder().id(1L).name("TRIP_VIEW").build();
        Role role = Role.builder().id(1L).name("DISPATCHER").permissions(Set.of(perm)).build();
        User user = User.builder()
                .id(10L)
                .username("dispatcher")
                .passwordHash("$2a$10$hash")
                .isActive(true)
                .roles(Set.of(role))
                .build();

        when(userRepository.findByUsernameAndIsActiveTrue("dispatcher")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("dispatcher");
        assertNotNull(details);
        assertEquals("dispatcher", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DISPATCHER")));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("TRIP_VIEW")));
    }

    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsernameAndIsActiveTrue("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown"));
    }
}
