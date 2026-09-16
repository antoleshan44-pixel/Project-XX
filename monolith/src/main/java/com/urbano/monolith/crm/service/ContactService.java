package com.urbano.monolith.crm.service;

import com.urbano.common.dto.PagedResponse;
import com.urbano.common.exception.ConflictException;
import com.urbano.common.exception.ResourceNotFoundException;
import com.urbano.monolith.crm.dto.ContactDto;
import com.urbano.monolith.crm.dto.ContactRequest;
import com.urbano.monolith.crm.entity.Contact;
import com.urbano.monolith.crm.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    // ============================================================
    // CREATE - Scoped to PM Account
    // ============================================================
    @Transactional
    public ContactDto createContact(ContactRequest request) {
        // Check if contact already exists
        if (contactRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        if (contactRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already exists: " + request.getPhone());
        }

        Contact contact = Contact.builder()
                .pmAccountId(request.getPmAccountId())  // ✅ Scoped
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .mobile(request.getMobile())
                .type(request.getType())
                .company(request.getCompany())
                .position(request.getPosition())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .preferredContactMethod(request.getPreferredContactMethod())
                .notes(request.getNotes())
                .assignedTo(request.getAssignedTo())
                .isActive(true)
                .source(request.getSource())
                .build();

        contact = contactRepository.save(contact);
        log.info("Contact created: {} for PM account {}", contact.getId(), contact.getPmAccountId());
        return mapToDto(contact);
    }

    // ============================================================
    // GET BY ID - With PM Account validation
    // ============================================================
    @Transactional(readOnly = true)
    public ContactDto getContact(UUID id, UUID pmAccountId) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // ✅ Validate PM Account ownership
        if (!contact.getPmAccountId().equals(pmAccountId)) {
            throw new SecurityException("Access denied: Contact does not belong to this PM account");
        }

        return mapToDto(contact);
    }

    // ============================================================
    // GET ALL - Scoped by PM Account
    // ============================================================
    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> getAllContacts(UUID pmAccountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> contactPage = contactRepository.findByPmAccountId(pmAccountId, pageable);
        return mapToPagedResponse(contactPage);
    }

    // ============================================================
    // GET ACTIVE CONTACTS - Scoped by PM Account
    // ============================================================
    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> getActiveContacts(UUID pmAccountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> contactPage = contactRepository.findByPmAccountIdAndIsActiveTrue(pmAccountId, pageable);
        return mapToPagedResponse(contactPage);
    }

    // ============================================================
    // GET BY TYPE - Scoped by PM Account
    // ============================================================
    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> getContactsByType(UUID pmAccountId, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> contactPage = contactRepository.findByPmAccountIdAndType(pmAccountId, type, pageable);
        return mapToPagedResponse(contactPage);
    }

    // ============================================================
    // SEARCH - Scoped by PM Account
    // ============================================================
    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> searchContacts(UUID pmAccountId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> contactPage = contactRepository.searchContacts(pmAccountId, search, pageable);
        return mapToPagedResponse(contactPage);
    }

    // ============================================================
    // UPDATE - With PM Account validation
    // ============================================================
    @Transactional
    public ContactDto updateContact(UUID id, UUID pmAccountId, ContactRequest request) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // ✅ Validate PM Account ownership
        if (!contact.getPmAccountId().equals(pmAccountId)) {
            throw new SecurityException("Access denied: Contact does not belong to this PM account");
        }

        // Check if email/phone conflict (excluding current contact)
        if (!contact.getEmail().equals(request.getEmail()) &&
                contactRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        if (!contact.getPhone().equals(request.getPhone()) &&
                contactRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone number already exists: " + request.getPhone());
        }

        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setMobile(request.getMobile());
        contact.setType(request.getType());
        contact.setCompany(request.getCompany());
        contact.setPosition(request.getPosition());
        contact.setAddress(request.getAddress());
        contact.setCity(request.getCity());
        contact.setState(request.getState());
        contact.setZipCode(request.getZipCode());
        contact.setCountry(request.getCountry());
        contact.setPreferredContactMethod(request.getPreferredContactMethod());
        contact.setNotes(request.getNotes());
        contact.setAssignedTo(request.getAssignedTo());
        contact.setSource(request.getSource());

        contact = contactRepository.save(contact);
        log.info("Contact updated: {}", contact.getId());
        return mapToDto(contact);
    }

    // ============================================================
    // DELETE (Soft Delete) - With PM Account validation
    // ============================================================
    @Transactional
    public void deleteContact(UUID id, UUID pmAccountId) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // ✅ Validate PM Account ownership
        if (!contact.getPmAccountId().equals(pmAccountId)) {
            throw new SecurityException("Access denied: Contact does not belong to this PM account");
        }

        contact.setActive(false);
        contact.setDeletedAt(LocalDateTime.now());
        contactRepository.save(contact);
        log.info("Contact deactivated: {}", id);
    }

    // ============================================================
    // UPDATE STATUS - With PM Account validation
    // ============================================================
    @Transactional
    public ContactDto updateContactStatus(UUID id, UUID pmAccountId, boolean active) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // ✅ Validate PM Account ownership
        if (!contact.getPmAccountId().equals(pmAccountId)) {
            throw new SecurityException("Access denied: Contact does not belong to this PM account");
        }

        contact.setActive(active);
        if (!active) {
            contact.setDeletedAt(LocalDateTime.now());
        }
        contact = contactRepository.save(contact);
        return mapToDto(contact);
    }

    // ============================================================
    // UPDATE LAST CONTACT DATE - With PM Account validation
    // ============================================================
    @Transactional
    public ContactDto updateLastContactDate(UUID id, UUID pmAccountId) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // ✅ Validate PM Account ownership
        if (!contact.getPmAccountId().equals(pmAccountId)) {
            throw new SecurityException("Access denied: Contact does not belong to this PM account");
        }

        contact.setLastContactDate(LocalDateTime.now());
        contact = contactRepository.save(contact);
        return mapToDto(contact);
    }

    // ============================================================
    // GET CONTACTS BY TYPE (Unscoped - for internal use only)
    // ============================================================
    @Deprecated
    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> getContactsByTypeUnscoped(String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Contact> contactPage = contactRepository.findByType(type, pageable);
        return mapToPagedResponse(contactPage);
    }

    // ============================================================
    // MAPPER METHODS
    // ============================================================
    private ContactDto mapToDto(Contact contact) {
        return ContactDto.builder()
                .id(contact.getId())
                .pmAccountId(contact.getPmAccountId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .fullName(contact.getFullName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .mobile(contact.getMobile())
                .type(contact.getType())
                .company(contact.getCompany())
                .position(contact.getPosition())
                .address(contact.getAddress())
                .city(contact.getCity())
                .state(contact.getState())
                .zipCode(contact.getZipCode())
                .country(contact.getCountry())
                .preferredContactMethod(contact.getPreferredContactMethod())
                .notes(contact.getNotes())
                .assignedTo(contact.getAssignedTo())
                .isActive(contact.isActive())
                .lastContactDate(contact.getLastContactDate())
                .source(contact.getSource())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private PagedResponse<ContactDto> mapToPagedResponse(Page<Contact> page) {
        List<ContactDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<ContactDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}