package com.university.attendance.security;

import com.university.attendance.models.User;
import com.university.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String prn) throws UsernameNotFoundException {
        User user = userRepository.findByPrnAndIsActiveTrue(prn)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive with PRN: " + prn));

        return new org.springframework.security.core.userdetails.User(
                user.getPrn(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
