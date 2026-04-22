package tn.esprit.sallesmateriels.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.sallesmateriels.entities.Materiel;
import tn.esprit.sallesmateriels.service.MaterielService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterielController.class)
class MaterielControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterielService materielService;

    @Test
    void getAll_returnsList() throws Exception {
        Materiel m = new Materiel();
        m.setId(1);
        m.setNom("Video");
        when(materielService.findAll()).thenReturn(List.of(m));

        mockMvc.perform(get("/api/materiels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Video"));
    }

    @Test
    void create_returns400WhenNomMissing() throws Exception {
        when(materielService.create(any(), isNull())).thenReturn(MaterielService.MaterielSaveOutcome.badRequest(null));

        mockMvc.perform(post("/api/materiels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns200WhenNomPresent() throws Exception {
        Materiel saved = new Materiel();
        saved.setId(5);
        saved.setNom("Retro");
        when(materielService.create(any(), isNull())).thenReturn(MaterielService.MaterielSaveOutcome.ok(saved, List.of()));

        mockMvc.perform(post("/api/materiels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Retro\",\"status\":\"DISPONIBLE\",\"quantiteTotale\":2,\"quantiteAssociee\":0,\"seuilMaintenance\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materiel.id").value(5))
                .andExpect(jsonPath("$.materiel.nom").value("Retro"));
    }
}
