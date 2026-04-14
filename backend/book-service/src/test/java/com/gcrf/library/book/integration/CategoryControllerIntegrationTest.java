package com.gcrf.library.book.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.book.dto.request.CategoryCreateRequest;
import com.gcrf.library.book.dto.request.CategoryUpdateRequest;
import com.gcrf.library.common.test.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CategoryController Integration Tests
 * Tests category CRUD operations and hierarchical tree operations
 *
 * @author GCRF Team
 * @date 2025-12-01
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("CategoryController Integration Tests")
@Sql(scripts = "/testdata/book-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CategoryControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/books/categories";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ==================== Get Category Tree ====================

    @Test
    @DisplayName("Should get category tree in tree mode")
    void testGetCategories_treeMode() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("treeMode", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].categoryName").exists())
                .andExpect(jsonPath("$.data[0].children").isArray());
    }

    @Test
    @DisplayName("Should get category list in non-tree mode")
    void testGetCategories_listMode() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("treeMode", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("Should get children categories by parent ID")
    void testGetCategories_byParentId() throws Exception {
        // Get children of Computer Science category (id=1000)
        mockMvc.perform(get(BASE_URL)
                        .param("parentId", "1000")
                        .param("treeMode", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data[0].parentId").value(1000));
    }

    @Test
    @DisplayName("Should return empty array for parent without children")
    void testGetCategories_noChildren() throws Exception {
        // Get children of a leaf category (id=1001 - Programming)
        mockMvc.perform(get(BASE_URL)
                        .param("parentId", "1001")
                        .param("treeMode", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Should get category tree showing hierarchical structure")
    void testGetCategories_hierarchicalStructure() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("treeMode", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.categoryCode=='CS')].categoryName").value("Computer Science"))
                .andExpect(jsonPath("$.data[?(@.categoryCode=='CS')].children").isArray())
                .andExpect(jsonPath("$.data[?(@.categoryCode=='CS')].children.length()").value(greaterThan(0)));
    }

    // ==================== Get Category Detail ====================

    @Test
    @DisplayName("Should get category detail by ID")
    void testGetCategory_success() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1000"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1000))
                .andExpect(jsonPath("$.data.categoryName").value("Computer Science"))
                .andExpect(jsonPath("$.data.categoryCode").value("CS"))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return error when category not found")
    void testGetCategory_notFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("Should get child category with parent information")
    void testGetCategory_childCategory() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.categoryName").value("Programming"))
                .andExpect(jsonPath("$.data.parentId").value(1000))
                .andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.path").value("1000.1001"));
    }

    // ==================== Create Category ====================

    @Test
    @DisplayName("Should create root category successfully")
    void testCreateCategory_rootCategory() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setCategoryName("Science");
        request.setCategoryCode("SCI");
        request.setDescription("Science and Nature");
        request.setStatus("ACTIVE");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.categoryName").value("Science"))
                .andExpect(jsonPath("$.data.categoryCode").value("SCI"))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.parentId").doesNotExist());
    }

    @Test
    @DisplayName("Should create child category successfully")
    void testCreateCategory_childCategory() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setParentId(1000L); // Computer Science
        request.setCategoryName("Database");
        request.setCategoryCode("CS.DB");
        request.setDescription("Database Systems");
        request.setStatus("ACTIVE");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.categoryName").value("Database"))
                .andExpect(jsonPath("$.data.categoryCode").value("CS.DB"))
                .andExpect(jsonPath("$.data.parentId").value(1000))
                .andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.path").value(startsWith("1000.")));
    }

    @Test
    @DisplayName("Should fail to create category with missing required fields")
    void testCreateCategory_missingRequiredFields() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        // Missing categoryName and categoryCode

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("Should fail to create category with duplicate category code")
    void testCreateCategory_duplicateCategoryCode() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setCategoryName("New Category");
        request.setCategoryCode("CS"); // Duplicate code
        request.setStatus("ACTIVE");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @DisplayName("Should fail to create category with non-existent parent")
    void testCreateCategory_invalidParent() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setParentId(99999L); // Non-existent parent
        request.setCategoryName("Invalid Category");
        request.setCategoryCode("INVALID");
        request.setStatus("ACTIVE");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== Update Category ====================

    @Test
    @DisplayName("Should update category successfully")
    void testUpdateCategory_success() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setId(1000L);
        request.setCategoryName("Computer Science and Technology");
        request.setDescription("Updated description for CS");
        request.setStatus("ACTIVE");

        mockMvc.perform(put(BASE_URL + "/1000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1000))
                .andExpect(jsonPath("$.data.categoryName").value("Computer Science and Technology"))
                .andExpect(jsonPath("$.data.description").value("Updated description for CS"));
    }

    @Test
    @DisplayName("Should fail to update non-existent category")
    void testUpdateCategory_notFound() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setId(99999L);
        request.setCategoryName("Non-existent Category");

        mockMvc.perform(put(BASE_URL + "/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("Should update category status to inactive")
    void testUpdateCategory_changeStatus() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();
        request.setId(1001L);
        request.setCategoryName("Programming");
        request.setStatus("INACTIVE");

        mockMvc.perform(put(BASE_URL + "/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    // ==================== Delete Category ====================

    @Test
    @DisplayName("Should delete leaf category successfully")
    void testDeleteCategory_leafCategory() throws Exception {
        // Delete a leaf category (no children)
        mockMvc.perform(delete(BASE_URL + "/1001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify category is deleted
        mockMvc.perform(get(BASE_URL + "/1001"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should fail to delete category with children")
    void testDeleteCategory_withChildren() throws Exception {
        // Try to delete Computer Science category which has children
        mockMvc.perform(delete(BASE_URL + "/1000"))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    @DisplayName("Should fail to delete non-existent category")
    void testDeleteCategory_notFound() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/99999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("Should fail to delete category with books")
    void testDeleteCategory_withBooks() throws Exception {
        // Try to delete Programming category which has books mapped to it
        mockMvc.perform(delete(BASE_URL + "/1001"))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle invalid category ID format")
    void testGetCategory_invalidIdFormat() throws Exception {
        mockMvc.perform(get(BASE_URL + "/invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should filter inactive categories from tree")
    void testGetCategories_filterInactive() throws Exception {
        // Test should verify that inactive categories are not shown by default
        mockMvc.perform(get(BASE_URL)
                        .param("treeMode", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.categoryCode=='INACTIVE')]").doesNotExist());
    }

    @Test
    @DisplayName("Should verify category path format")
    void testGetCategory_pathFormat() throws Exception {
        // Verify path follows the correct format: parent.child
        mockMvc.perform(get(BASE_URL + "/1001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.path").value(matchesPattern("\\d+\\.\\d+")));
    }

    @Test
    @DisplayName("Should verify category book count statistics")
    void testGetCategory_bookCountStatistics() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookCount").isNumber())
                .andExpect(jsonPath("$.data.bookCount").value(greaterThanOrEqualTo(0)));
    }
}
