package edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto;

import java.math.BigDecimal;

public class PistaPatchRequest {
    private String nombre;
    private String ubicacion;
    private BigDecimal precioHora;
    private Boolean activa;

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public BigDecimal getPrecioHora() { return precioHora; }
    public void setPrecioHora(BigDecimal precioHora) { this.precioHora = precioHora; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
