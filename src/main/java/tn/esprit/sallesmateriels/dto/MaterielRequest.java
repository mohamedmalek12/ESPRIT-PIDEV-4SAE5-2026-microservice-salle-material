package tn.esprit.sallesmateriels.dto;

/**
 * DTO pour créer ou modifier un matériel (nom + status uniquement dans le body).
 */
public class MaterielRequest {
    private String nom;
    private String status;
    private Double dureeUtilisation;
    private Double seuilMaintenance;
    private Integer quantiteTotale;
    private Integer quantiteAssociee;

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
}
