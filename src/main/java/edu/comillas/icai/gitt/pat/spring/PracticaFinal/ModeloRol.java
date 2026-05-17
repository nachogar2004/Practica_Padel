package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import lombok.Getter;

/**
 * Enumeración que define los roles posibles de un usuario en el sistema.
 *
 * <p>Un enum es una clase especial de Java que representa un conjunto fijo de
 * constantes. En este caso, un usuario SIEMPRE tiene uno de estos dos roles.</p>
 *
 * <h3>Decisión de diseño</h3>
 * <ul>
 *   <li>Se usa un enum en lugar de un String libre para garantizar que solo
 *       existan valores válidos; si alguien intenta asignar "SUPERADMIN" el
 *       compilador lo rechazará.</li>
 *   <li>Cada valor lleva un campo {@code descripcion} con texto legible para
 *       humanos, útil para logs y mensajes de error.</li>
 *   <li>La anotación {@code @Getter} de Lombok genera automáticamente el
 *       método {@code getDescripcion()} sin necesidad de escribirlo a mano.</li>
 * </ul>
 *
 * <h3>Cómo se usa en la base de datos</h3>
 * <p>En {@link ModeloUsuario} el campo rol está anotado con
 * {@code @Enumerated(EnumType.STRING)}, por lo que Hibernate guarda la cadena
 * "USER" o "ADMIN" en lugar del índice numérico. Esto hace la BD legible.</p>
 */
@Getter

public enum ModeloRol {

    /** Rol para clientes normales: pueden reservar pistas, consultar disponibilidad, etc. */
    USER("Usuario normal"),

    /** Rol para administradores: pueden crear/editar/eliminar pistas y ver todas las reservas.
     *  Los ADMIN no pueden hacer reservas propias (restricción de negocio). */
    ADMIN("Administrador");

    /** Texto descriptivo del rol, pensado para mostrarse en logs o mensajes. */
    private final String descripcion;

    /**
     * Constructor del enum. En Java los enums pueden tener campos y constructores.
     *
     * @param descripcion texto legible que describe el rol
     */
    ModeloRol(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el texto descriptivo del rol.
     * Este método lo genera Lombok con @Getter, pero se deja explícito para claridad.
     *
     * @return descripción textual del rol
     */
    public String getDescripcion() {
        return descripcion;
    }
}
