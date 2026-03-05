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

    @ManyToOne
    @JoinColumn(name = "salle_id")
    @JsonIgnoreProperties("materiels")
    private Salle salle;

    public Materiel() {
    }

    public Materiel(Integer id, String nom, MaterielStatus status, Salle salle) {
        this.id = id;
        this.nom = nom;
        this.status = status;
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
}