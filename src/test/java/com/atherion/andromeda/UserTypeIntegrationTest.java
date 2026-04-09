package com.atherion.andromeda;

import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.repositories.UserTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserTypeIntegrationTest {

    @Autowired
    private UserTypeRepository userTypeRepository;

    @Test
    void writeAndReadUserType() {
        // Write
        UserType userType = new UserType();
        userType.setUserType("developer");
        userType.setDescription("Software developer role");
        UserType saved = userTypeRepository.save(userType);

        assertNotNull(saved.getId());

        // Read
        Optional<UserType> found = userTypeRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("developer", found.get().getUserType());

        // Cleanup
        userTypeRepository.deleteById(saved.getId());
    }
}