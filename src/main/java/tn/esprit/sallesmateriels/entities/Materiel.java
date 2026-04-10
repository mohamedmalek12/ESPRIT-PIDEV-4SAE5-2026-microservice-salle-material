package tn.esprit.sallesmateriels.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Materiel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nom;

    @Enumerated(EnumType.STRING)
    private MaterielStatus status;

    private Double dureeUtilisation = 0.0;

    private Double seuilMaintenance = 100.0;

    private Integer quantiteTotale = 1;

    private Integer quantiteAssociee = 0;

    @ManyToOne
    @JoinColumn(name = "salle_id")
    @JsonIgnoreProperties("materiels")
    private Salle salle;

    public Materiel() {
    }

    public Materiel(Integer id, String nom, MaterielStatus status, Double dureeUtilisation, Double seuilMaintenance,
                    Integer quantiteTotale, Integer quantiteAssociee, Salle salle) {
        this.id = id;
        this.nom = nom;
        this.status = status;
        this.dureeUtilisation = dureeUtilisation;
        this.seuilMaintenance = seuilMaintenance;
        this.quantiteTotale = quantiteTotale;
        this.quantiteAssociee = quantiteAssociee;
        this.salle = salle;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public MaterielStatus getStatus() {
        return status;
    }

    public void setStatus(MaterielStatus status) {
        this.status = status;
    }

    public Salle getSalle() {
        return salle;
    }

    public void setSalle(Salle salle) {
        this.salle = salle;
    }

    public Double getDureeUtilisation() {
        return dureeUtilisation;
    }

    public void setDureeUtilisation(Double dureeUtilisation) {
        this.dureeUtilisation = dureeUtilisation;
    }

    public Double getSeuilMaintenance() {
        return seuilMaintenance;
    }

    public void setSeuilMaintenance(Double seuilMaintenance) {
        this.seuilMaintenance = seuilMaintenance;
    }

    public Integer getQuantiteTotale() {
        return quantiteTotale;
    }

    public void setQuantiteTotale(Integer quantiteTotale) {
        this.quantiteTotale = quantiteTotale;
    }

    public Integer getQuantiteAssociee() {
        return quantiteAssociee;
    }

    public void setQuantiteAssociee(Integer quantiteAssociee) {
        this.quantiteAssociee = quantiteAssociee;
    }

    /**
     * Quantité restante (non persistée) : total - assigné. Exposée en JSON comme {@code quantiteRestante}.
     */
    public Integer getQuantiteRestante() {
        int total = quantiteTotale != null ? quantiteTotale : 0;
        int assoc = quantiteAssociee != null ? quantiteAssociee : 0;
        return Math.max(0, total - assoc);
    }
}