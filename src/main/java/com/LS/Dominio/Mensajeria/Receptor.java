package com.LS.Dominio.Mensajeria;

import DTO.*;
import ObjetoValor.EstadoReserva;
import com.LS.Dominio.Entidad.*;
import com.LS.Dominio.Parser.EspacioParser;
import com.LS.Dominio.Parser.ReservaParser;
import com.LS.Dominio.Servicio.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.rabbitmq.client.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class Receptor{

    @Autowired
    GestionReservas gestionReservas;

    @Autowired
    ObtenerReservas obtenerReservas;

    @Autowired
    ObtenerHorarios obtenerHorarios;

    @Autowired
    ObtenerEspacios obtenerEspacios;

    @Autowired
    ModificarEspacio modificarEspacio;

    @Autowired
    ReservaParser reservaParser;

    @Autowired
    EspacioParser espacioParser;

    private Logger log = LoggerFactory.getLogger(Receptor.class);

    private final static String COLA_ENTRADA = "entrada";
    private final static String COLA_SALIDA = "salida";
    private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";
    private Channel canal;

    public Receptor() throws Exception {
        ConnectionFactory factoria = new ConnectionFactory();
        String amqpURL = System.getenv(ENV_AMQPURL_NAME) != null ?
                System.getenv().get(ENV_AMQPURL_NAME) : "amqp://localhost";
        try {
            factoria.setUri(amqpURL);
        }  catch (Exception e) {
            System.out.println(" [*] AQMP broker no encontrado en " + amqpURL);
            System.exit(-1);
        }
        Connection conexion = factoria.newConnection();
        canal = conexion.createChannel();
        canal.queueDeclare(COLA_ENTRADA, false, false, false, null);
        canal.queueDeclare(COLA_SALIDA, false, false, false, null);
    }

    public void esperarMensajes() throws Exception {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
            log.info(" [x] Recibido: '"+ mensaje + "'");
            try {
                llamarAServicio(mensaje);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        canal.basicConsume(COLA_ENTRADA, true, deliverCallback, consumerTag -> { });
        log.info("Esperando mensajes...");
    }

    public void devolverMensajes(String mensaje) throws Exception {
        canal.basicPublish("", COLA_SALIDA, null, mensaje.getBytes());
        log.info(" [x] Enviado '" + mensaje + "'");
    }

    public void llamarAServicio(String mensaje) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONObject jsonObject;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String[] mensajeArray = mensaje.split(",", 2);

        switch (mensajeArray[0]) {
            case "crearReserva":
                devolverMensajes(mapper.writeValueAsString(reservaParser
                        .entidadADTO(gestionReservas.crear(reservaParser
                        .DTOAEntidad(mapper.readValue(mensajeArray[1], ReservaDTO.class))))));
            break;

            case "modificarEstadoReserva":
                jsonObject = new JSONObject(mensajeArray[1]);
                Optional<Reserva> reservaOptional = gestionReservas.cambiarEstado(
                        jsonObject.getString("id"),
                        EstadoReserva.valueOf(jsonObject.getString("estado")),
                        jsonObject.getString("motivo"));
                if (reservaOptional.isPresent()) {
                    devolverMensajes(mapper.writeValueAsString(
                            reservaParser.entidadADTO(reservaOptional.get())));
                } else {
                    devolverMensajes("ERROR");
                }
            break;

            case "obtenerReservasEspacio":
                jsonObject = new JSONObject(mensajeArray[1]);
                Collection<Reserva> reservasEspacio = obtenerReservas
                        .obtenerReservasEspacio(jsonObject.getString("idEspacio"));
                devolverMensajes(mapper.writeValueAsString(reservasEspacio
                        .stream()
                        .map(reservaParser::entidadADTO)
                        .collect(Collectors.toList())));
            break;

            //??
            case "obtenerReservasEspacioFecha":
                jsonObject = new JSONObject(mensajeArray[1]);
                Collection<Reserva> reservasEspacioFecha = obtenerReservas
                        .obtenerPorEspacioYFecha(jsonObject.getString("idEspacio"),
                                new Timestamp(jsonObject.getLong("fecha")));
                devolverMensajes(mapper.writeValueAsString(reservasEspacioFecha
                        .stream()
                        .map(reservaParser::entidadADTO)
                        .collect(Collectors.toList())));
            break;

            case "obtenerHorarioEntreFechas":
                jsonObject = new JSONObject(mensajeArray[1]);
                devolverMensajes(mapper.writeValueAsString(obtenerHorarios
                        .obtenerPorEspacioEntreFechas(jsonObject.getString("idEspacio"),
                                new Timestamp(jsonObject.getLong("fechaInicio")),
                                new Timestamp(jsonObject.getLong("fechaFin")))));
                break;

            case "obtenerEspacioPorId:":
                jsonObject = new JSONObject(mensajeArray[1]);
                Optional<Espacio> espacioOptional = obtenerEspacios
                        .obtenerInformacion(jsonObject.getString("id"));
                if (espacioOptional.isPresent()) {
                    devolverMensajes(mapper.writeValueAsString(espacioParser.
                            entidadADTO(espacioOptional.get())));
                } else {
                    devolverMensajes("ERROR");
                }
                break;

            case "obtenerEspacioPorEdificioYTipo:":
                jsonObject = new JSONObject(mensajeArray[1]);
                Collection<Espacio> espaciosPorEdificioYTipo = obtenerEspacios
                        .obtenerPorEdificioYTipo(jsonObject.getString("edificio"),
                                jsonObject.getString("tipo"));
                devolverMensajes(mapper.writeValueAsString(espaciosPorEdificioYTipo
                        .stream()
                        .map(espacioParser::entidadADTO)
                        .collect(Collectors.toList())));
                break;

            case "modificarEspacio:":
                jsonObject = new JSONObject(mensajeArray[1]);
                try {

                    Optional<Integer> capacidad;
                    if (jsonObject.getString("capacidad").equals("null")) {
                        capacidad = Optional.empty();
                    } else {
                        capacidad = Optional.of(Integer.parseInt(jsonObject.getString("capacidad")));
                    }

                    Optional<String> notas;
                    if (jsonObject.getString("notas").equals("null")) {
                        notas = Optional.empty();
                    } else {
                        notas = Optional.of(jsonObject.getString("notas"));
                    }

                    Optional<Espacio> espacioModificadoOptional = modificarEspacio
                            .modificar(jsonObject.getString("id"),
                                    capacidad, notas);
                    if (espacioModificadoOptional.isPresent()) {
                        devolverMensajes(mapper.writeValueAsString(espacioParser.
                                entidadADTO(espacioModificadoOptional.get())));
                    } else {
                        devolverMensajes("ERROR");
                    }
                } catch (Exception e){
                    devolverMensajes("ERROR");
                }
                break;

            default:
                devolverMensajes("Mensaje mal formado");
            break;
        }
    }
}
