package chnu.edu.inno;

/**
 * @author Rout
 * @version 1.0.0
 * @project inno
 * @class ItemAuditIntegrationTest
 * @since 29.10.2026 - 23.14
 */

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ItemAuditIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void itemLifecycleIsAudited() throws Exception {
        // a) create
        String created = mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Apple","code":"000009","quantity":5,"description":"green apple"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdBy").value("george"))
                .andExpect(jsonPath("$.createdDate").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        String createdDate = JsonPath.read(created, "$.createdDate");

        // c) update: name and quantity change, createdDate stays, lastModifiedDate moves
        Thread.sleep(20);
        mockMvc.perform(put("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"%s","name":"Apple Red","code":"000009","quantity":7,"description":"green apple"}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple Red"))
                .andExpect(jsonPath("$.quantity").value(7))
                .andExpect(jsonPath("$.createdBy").value("george"))
                .andExpect(jsonPath("$.createdDate").value(createdDate))
                .andExpect(jsonPath("$.lastModifiedDate").value(not(createdDate)));

        // d) update of a missing item gives 404 and leaves no trace in Javers
        mockMvc.perform(put("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"nonexistent123","name":"X","code":"1","quantity":1,"description":"x"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/audit/shadows/nonexistent123"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // e) the audit shows the creation and the changes of name and quantity
        mockMvc.perform(get("/audit/changes/item/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("new object")))
                .andExpect(content().string(containsString("'name' changed: 'Apple' -> 'Apple Red'")))
                .andExpect(content().string(containsString("'quantity' changed: '5' -> '7'")))
                .andExpect(content().string(not(containsString("'createdDate' changed"))));

        // g) delete gives 204, the second delete gives 404
        mockMvc.perform(delete("/api/v1/items/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/items/" + id)).andExpect(status().isNotFound());

        // h) deletion is visible in the audit as a terminal snapshot
        mockMvc.perform(get("/audit/changes/item/" + id))
                .andExpect(content().string(containsString("object removed")));
    }
}
