package tn.esprit.sallesmateriels.dto;

/**
 * DTO pour la création et mise à jour d'une salle (évite les erreurs 400 de désérialisation).
 */
public class SalleRequest {
    private String nom;
    private Integer capacite;

    public SalleRequest() {
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }
}
