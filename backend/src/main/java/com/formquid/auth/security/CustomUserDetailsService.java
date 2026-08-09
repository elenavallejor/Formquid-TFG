package com.formquid.auth.security;

import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio utilizado por Spring Security para cargar usuarios desde la base
 * de datos durante el proceso de autenticación.
 *
 * Busca usuarios por correo electrónico y los adapta al modelo
 * {@link CustomUserDetails}.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga un usuario a partir de su correo electrónico.
     *
     * @param email correo electrónico utilizado en el login.
     * @return datos del usuario en formato compatible con Spring Security.
     * @throws UsernameNotFoundException si no existe ningún usuario con ese correo.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .map(CustomUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}