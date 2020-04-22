/*
 * EspacioService.java 1.0 30/03/2020
 */

/**
 * Esta clase contiene los servicios que gestionan las reservas
 *
 * @author Gonzalo Berné
 * @version 2.0, 22/04/2020
 */

package com.LS.Dominio.Servicio;

import ObjetoValor.EstadoReserva;
import com.LS.Dominio.Entidad.Reserva;
import com.LS.Dominio.Repositorio.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GestionReservas {

    @Autowired
    private ReservaRepository reservaRepository;

    public Reserva crear(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    public Optional<Reserva> cambiarEstado(String id, EstadoReserva estado, String motivo) {
        Optional<Reserva> reservaOptional = reservaRepository.findById(id);
        reservaOptional.ifPresent(reserva -> {
            reserva.setEstado(estado);
            reservaRepository.save(reserva);
            //ENVIAR EMAIL CON EL MOTIVO AL USUARIO DE LA RESERVA
        });
        return reservaOptional;
    }

}

