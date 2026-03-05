package tn.esprit.sallesmateriels.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.sallesmateriels.entities.Salle;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Integer> {
}

