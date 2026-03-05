package tn.esprit.sallesmateriels.dto;

/**
 * DTO pour créer ou modifier un matériel (nom + status uniquement dans le body).
 */
public class MaterielRequest {
    private String nom;
    private String status;

    public MaterielRequest() {
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
