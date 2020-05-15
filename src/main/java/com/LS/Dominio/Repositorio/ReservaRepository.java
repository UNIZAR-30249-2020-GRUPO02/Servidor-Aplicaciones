/*
 * ReservaRepository.java 1.0 30/03/2020
 */

/**
 * Esta interfaz es el repositorio de Reserva
 * utilizado para mantener persistencia de la entidad Reserva
 *
 * @author Gonzalo Berné
 * @version 1.0, 30/03/2020
 */

package com.LS.Dominio.Repositorio;

import com.LS.Dominio.Entidad.Reserva;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;

public interface ReservaRepository extends CrudRepository<Reserva, String> {

    List<Reserva> findByIdEspacio(String idEspacio);

    List<Reserva> findByEstado(String estado);

    List<Reserva> findByIdEspacioAndEstado(String idEspacio, String estado);

    // ??
    @Query(value = "SELECT r FROM Reserva r WHERE r.idEspacio = ?1 " +
            "AND r.horaInicio > ?2 AND r.horaInicio < ?3")
    List<Reserva> findByIdEspacioYEntreDosFechas(String idEspacio, Timestamp dia, Timestamp diaSiguiente);

    @Query(value = "SELECT r FROM Reserva r WHERE r.idEspacio = ?1 " +
            "AND r.fechaInicio <= ?2 AND r.fechaFin >= ?2 AND ?3 MEMBER OF r.dias")
    List<Reserva> findByIdEspacioYDia(String idEspacio, Timestamp dia, Integer diaSemana);

    @Query(value = "SELECT r FROM Reserva r WHERE r.idEspacio = ?1 AND r.estado = ?2 " +
            "AND r.fechaInicio >= ?3 AND r.fechaFin <= ?4 AND r.horaInicio >= ?5 AND r.horaFin <= ?6")
    List<Reserva> findByEspacioFechasYHoras(String idEspacio, String  estado, Timestamp fechaIni, Timestamp fechaFin,
                                Timestamp horaIni, Timestamp horaFin);
}
