package com.formquid.auth.security;

import com.formquid.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapta la entidad User al modelo utilizado por Spring Security.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * Crea el objeto de seguridad a partir de un usuario registrado.
     *
     * @param user usuario de FormQuid.
     */
    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Devuelve el identificador del usuario.
     *
     * @return id numérico del usuario.
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Devuelve la entidad de usuario asociada.
     *
     * @return usuario autenticado.
     */
    public User getUser() {
        return user;
    }

    /**
     * Devuelve el rol del usuario en formato compatible con Spring Security.
     *
     * @return autoridades del usuario.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    /**
     * Devuelve la contraseña cifrada.
     *
     * @return hash de la contraseña.
     */
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Devuelve el correo usado como nombre de usuario.
     *
     * @return email del usuario.
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Indica si la cuenta no ha expirado.
     *
     * @return true.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta no está bloqueada.
     *
     * @return true si la cuenta está activa.
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.isEnabled();
    }

    /**
     * Indica si las credenciales no han expirado.
     *
     * @return true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si el usuario está habilitado.
     *
     * @return true si el usuario está activo.
     */
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}