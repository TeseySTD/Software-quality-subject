package chnu.edu.inno.controller;

/**
 * @author Rout
 * @version 1.0.0
 * @project inno
 * @class AuditController
 * @since 29.10.2026 - 23.09
 */

import chnu.edu.inno.model.Item;
import lombok.AllArgsConstructor;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/audit")
public class AuditController {

    private final Javers javers;

    @GetMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getItemChanges() {
        QueryBuilder jqlQuery = QueryBuilder.byClass(Item.class);
        Changes changes = javers.findChanges(jqlQuery.build());
        return javers.getJsonConverter().toJson(changes);
    }

    @GetMapping(value = "/shadows", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getItemShadows() {
        QueryBuilder jqlQuery = QueryBuilder.byClass(Item.class);
        return javers.getJsonConverter().toJson(javers.findShadows(jqlQuery.build()));
    }

    @GetMapping(value = "/shadows/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getItemShadowsById(@PathVariable String id) {
        QueryBuilder jqlQuery = QueryBuilder.byInstanceId(id, Item.class).withNewObjectChanges();
        return javers.getJsonConverter().toJson(javers.findShadows(jqlQuery.build()));
    }

    @GetMapping(value = "/changes/item/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String getItemChangesById(@PathVariable String id) {
        QueryBuilder jqlQuery = QueryBuilder.byInstanceId(id, Item.class).withNewObjectChanges();
        Changes changes = javers.findChanges(jqlQuery.build());
        return "<pre>" + changes.prettyPrint() + "</pre>";
    }
}
