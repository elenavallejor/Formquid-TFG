package com.formquid.registration.scheduler;

import com.formquid.registration.service.RegistrationRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea de mantenimiento para las solicitudes de registro.
 *
 * Las solicitudes resueltas se conservan durante siete días para permitir su
 * consulta administrativa y, una vez expirado ese periodo, se eliminan de
 * forma automática mediante una única operación masiva en base de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationRequestCleanupScheduler {

    private final RegistrationRequestService registrationRequestService;

    /**
     * Ejecuta la limpieza al comienzo de cada hora.
     *
     * Una solicitud se eliminará en la primera ejecución posterior a cumplir
     * siete días desde resolvedAt, por lo que el desfase máximo es de una hora.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void deleteExpiredRegistrationRequests() {
        int deleted =
            registrationRequestService
                .deleteExpiredResolvedRequests();

        if (deleted > 0) {
            log.info(
                "Eliminadas {} solicitudes de registro resueltas con más de 7 días",
                deleted
            );
        }
    }
}
