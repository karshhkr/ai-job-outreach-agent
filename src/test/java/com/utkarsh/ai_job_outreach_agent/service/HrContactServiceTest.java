
package com.utkarsh.ai_job_outreach_agent.service;

import com.utkarsh.ai_job_outreach_agent.dto.HrContactRequest;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.repository.HrContactRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrContactServiceTest {

    @Mock
    private HrContactRepository repository;

    @InjectMocks
    private HrContactService hrContactService;

    private HrContact contact;
    private HrContactRequest request;

    @BeforeEach
    void setUp() {

        contact = new HrContact();
        contact.setId(1L);
        contact.setName("Rahul Sharma");
        contact.setEmail("rahul@example.com");
        contact.setCompany("Google");
        contact.setJobTitle("HR Manager");

        request = new HrContactRequest(
                "Rahul Sharma",
                "rahul@example.com",
                "Google",
                "HR Manager",
                "https://linkedin.com/in/rahul",
                "Java hiring"
        );
    }

    // 1. Find all contacts
    @Test
    void findAll_shouldReturnAllContacts() {

        when(repository.findAll()).thenReturn(List.of(contact));

        List<HrContact> result = hrContactService.findAll();

        assertEquals(1, result.size());
        assertEquals("Rahul Sharma", result.get(0).getName());

        verify(repository).findAll();
    }

    // 2. Create contact successfully
    @Test
    void create_shouldSaveContact_whenEmailDoesNotExist() {

        when(repository.findByEmailIgnoreCase("rahul@example.com"))
                .thenReturn(Optional.empty());

        when(repository.save(any(HrContact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HrContact result = hrContactService.create(request);

        assertNotNull(result);
        assertEquals("Rahul Sharma", result.getName());
        assertEquals("rahul@example.com", result.getEmail());
        assertEquals("Google", result.getCompany());

        verify(repository).save(any(HrContact.class));
    }

    // 3. Duplicate email should throw exception
    @Test
    void create_shouldThrowException_whenEmailAlreadyExists() {

        when(repository.findByEmailIgnoreCase("rahul@example.com"))
                .thenReturn(Optional.of(contact));

        assertThrows(
                IllegalArgumentException.class,
                () -> hrContactService.create(request)
        );

        verify(repository, never()).save(any(HrContact.class));
    }

    // 4. Email should be trimmed and lowercase
    @Test
    void create_shouldNormalizeEmail() {

        HrContactRequest newRequest = new HrContactRequest(
                "Rahul Sharma",
                "  RAHUL@EXAMPLE.COM  ",
                "Google",
                "HR Manager",
                null,
                null
        );

        when(repository.findByEmailIgnoreCase("rahul@example.com"))
                .thenReturn(Optional.empty());

        when(repository.save(any(HrContact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HrContact result = hrContactService.create(newRequest);

        assertEquals("rahul@example.com", result.getEmail());

        verify(repository).save(any(HrContact.class));
    }

    // 5. Update existing contact
    @Test
    void update_shouldUpdateContact_whenContactExists() {

        HrContactRequest updateRequest = new HrContactRequest(
                "Rahul Updated",
                "rahul@example.com",
                "Microsoft",
                "Senior HR",
                null,
                "Updated notes"
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(contact));

        when(repository.findByEmailIgnoreCase("rahul@example.com"))
                .thenReturn(Optional.of(contact));

        when(repository.save(any(HrContact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HrContact result = hrContactService.update(1L, updateRequest);

        assertEquals("Rahul Updated", result.getName());
        assertEquals("Microsoft", result.getCompany());
        assertEquals("Senior HR", result.getJobTitle());

        verify(repository).save(contact);
    }

    // 6. Update should fail when contact does not exist
    @Test
    void update_shouldThrowException_whenContactDoesNotExist() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> hrContactService.update(1L, request)
        );

        verify(repository, never()).save(any(HrContact.class));
    }

    // 7. Update should fail when email belongs to another contact
    @Test
    void update_shouldThrowException_whenEmailBelongsToAnotherContact() {

        HrContact anotherContact = new HrContact();
        anotherContact.setId(2L);
        anotherContact.setEmail("rahul@example.com");

        when(repository.findById(1L))
                .thenReturn(Optional.of(contact));

        when(repository.findByEmailIgnoreCase("rahul@example.com"))
                .thenReturn(Optional.of(anotherContact));

        assertThrows(
                IllegalArgumentException.class,
                () -> hrContactService.update(1L, request)
        );

        verify(repository, never()).save(any(HrContact.class));
    }

    // 8. Delete existing contact
    @Test
    void delete_shouldDeleteContact_whenContactExists() {

        when(repository.existsById(1L)).thenReturn(true);

        hrContactService.delete(1L);

        verify(repository).deleteById(1L);
    }

    // 9. Delete should fail when contact does not exist
    @Test
    void delete_shouldThrowException_whenContactDoesNotExist() {

        when(repository.existsById(1L)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> hrContactService.delete(1L)
        );

        verify(repository, never()).deleteById(1L);
    }

    // 10. Get contact by ID
    @Test
    void get_shouldReturnContact_whenContactExists() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(contact));

        HrContact result = hrContactService.get(1L);

        assertNotNull(result);
        assertEquals("Rahul Sharma", result.getName());

        verify(repository).findById(1L);
    }

    // 11. Get should fail when contact does not exist
    @Test
    void get_shouldThrowException_whenContactDoesNotExist() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> hrContactService.get(1L)
        );

        verify(repository).findById(1L);
    }
}
