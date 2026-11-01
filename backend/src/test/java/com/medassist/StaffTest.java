package com.medassist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inviting staff, accepting, and what each role may then do. */
class StaffTest extends IntegrationTest {

    @Test
    void invitedPharmacistJoinsWithTheRightPermissions() throws Exception {
        Session owner = registerPharmacy("Staff " + uniq);
        String email = "pharmacist-" + uniq + "@test.in";

        String invite = json(mvc.perform(jsonPost("/api/v1/staff/invitations",
                        "{\"email\":\"" + email + "\",\"role\":\"PHARMACIST\"}").with(owner))
                .andExpect(status().isCreated()).andReturn());
        String token = ((String) JsonPath.read(invite, "$.acceptUrl")).replaceAll(".*token=", "");

        mvc.perform(get("/api/v1/invitations/preview").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgName").value("Staff " + uniq))
                .andExpect(jsonPath("$.role").value("PHARMACIST"));

        // Someone else who got hold of the link cannot use it.
        Session stranger = registerConsumer("Stranger");
        mvc.perform(jsonPost("/api/v1/invitations/accept", "{\"token\":\"" + token + "\"}").with(stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVITE_EMAIL_MISMATCH"));

        Session invited = register(email, "Priya Pharmacist");
        MvcResult accepted = mvc.perform(jsonPost("/api/v1/invitations/accept", "{\"token\":\"" + token + "\"}")
                        .with(invited))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeOrg.role").value("PHARMACIST"))
                .andReturn();
        Session pharmacist = new Session(JsonPath.read(json(accepted), "$.accessToken"),
                JsonPath.read(json(accepted), "$.refreshToken"), email, owner.orgId());

        mvc.perform(get("/api/v1/products").with(pharmacist)).andExpect(status().isOk());          // CASHIER+
        mvc.perform(jsonPost("/api/v1/products", """
                        {"name":"Pan 40","schedule":"OTC","gstRate":5,"mrp":150}""").with(pharmacist))
                .andExpect(status().isCreated());                                                   // PHARMACIST+
        mvc.perform(jsonPost("/api/v1/staff/invitations", "{\"email\":\"x-" + uniq + "@test.in\",\"role\":\"CASHIER\"}")
                .with(pharmacist)).andExpect(status().isForbidden());                               // ADMIN+
        mvc.perform(jsonPost("/api/v1/billing/plan", "{\"plan\":\"FREE\"}").with(pharmacist))
                .andExpect(status().isForbidden());                                                 // OWNER

        // The invite is single use.
        mvc.perform(jsonPost("/api/v1/invitations/accept", "{\"token\":\"" + token + "\"}").with(invited))
                .andExpect(status().is4xxClientError());

        List<String> roles = JsonPath.read(json(mvc.perform(get("/api/v1/staff").with(owner)).andReturn()),
                "$.members[*].role");
        assertThat(roles).containsExactlyInAnyOrder("OWNER", "PHARMACIST");
    }

    @Test
    void adminsCannotTouchTheOwner() throws Exception {
        Session owner = registerPharmacy("Guard " + uniq);
        String email = "admin-" + uniq + "@test.in";
        String invite = json(mvc.perform(jsonPost("/api/v1/staff/invitations",
                "{\"email\":\"" + email + "\",\"role\":\"ADMIN\"}").with(owner)).andReturn());
        String token = ((String) JsonPath.read(invite, "$.acceptUrl")).replaceAll(".*token=", "");
        Session invited = register(email, "Arun Admin");
        String accepted = json(mvc.perform(jsonPost("/api/v1/invitations/accept", "{\"token\":\"" + token + "\"}")
                .with(invited)).andExpect(status().isOk()).andReturn());
        Session admin = new Session(JsonPath.read(accepted, "$.accessToken"), JsonPath.read(accepted, "$.refreshToken"),
                email, owner.orgId());

        List<String> owners = JsonPath.read(json(mvc.perform(get("/api/v1/staff").with(owner)).andReturn()),
                "$.members[?(@.role == 'OWNER')].membershipId");
        String ownerMembership = owners.getFirst();
        mvc.perform(patch("/api/v1/staff/" + ownerMembership + "/role").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"CASHIER\"}"))
                .andExpect(status().isForbidden());
    }

    private Session register(String email, String name) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"secret123\",\"fullName\":\"%s\"}".formatted(email, name)))
                .andExpect(status().isCreated()).andReturn();
        return new Session(JsonPath.read(json(res), "$.accessToken"), JsonPath.read(json(res), "$.refreshToken"),
                email, null);
    }
}
