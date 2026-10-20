package com.formquid.user.dto;

import com.formquid.user.entity.User;

/**
 * Devuelve el usuario actualizado e indica si debe renovarse el token de sesión.
 */
public record UpdateAccountResult(
    User user,
    boolean tokenRenewalRequired
) {
}
