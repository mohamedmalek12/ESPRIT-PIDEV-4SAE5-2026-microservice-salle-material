package tn.esprit.sallesmateriels.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.sallesmateriels.entities.Materiel;

@Repository
public interface MaterielRepository extends JpaRepository<Materiel, Integer> {

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM materiel WHERE id = :id", nativeQuery = true)
    void deleteByIdNative(@Param("id") Integer id);

    java.util.List<Materiel> findBySalleId(Integer salleId);
}
