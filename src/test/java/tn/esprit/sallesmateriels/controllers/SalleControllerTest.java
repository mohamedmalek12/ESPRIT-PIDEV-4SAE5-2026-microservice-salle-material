package tn.esprit.sallesmateriels.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.sallesmateriels.entities.Salle;
import tn.esprit.sallesmateriels.service.SalleService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalleController.class)
class SalleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalleService salleService;

    @Test
    void getAll_returnsOkAndJsonArray() throws Exception {
        Salle s = new Salle();
        s.setId(1);
        s.setNom("A104");
        s.setCapacite(30);
        when(salleService.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/salles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("A104"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        when(salleService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/salles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsSavedSalle() throws Exception {
        when(salleService.create(any(Map.class))).thenAnswer(inv -> {
            Map<?, ?> req = inv.getArgument(0);
            Salle out = new Salle();
            out.setId(2);
            Object nom = req.get("nom");
            Object capacite = req.get("capacite");
            out.setNom(nom != null ? nom.toString().trim() : "");
            out.setCapacite(capacite instanceof Number n ? n.intValue() : 1);
            return out;
        });

        mockMvc.perform(post("/api/salles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\" B203 \",\"capacite\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nom").value("B203"))
                .andExpect(jsonPath("$.capacite").value(25));
    }
}
