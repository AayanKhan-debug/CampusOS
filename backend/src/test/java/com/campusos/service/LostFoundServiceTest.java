package com.campusos.service;

import com.campusos.dto.request.CreateLostFoundRequest;
import com.campusos.dto.request.UpdateLostFoundRequest;
import com.campusos.dto.request.UpdateLostFoundStatusRequest;
import com.campusos.dto.response.LostFoundResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.LostFoundItem;
import com.campusos.entity.User;
import com.campusos.enums.ListingType;
import com.campusos.enums.LostFoundStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.LostFoundRepository;
import com.campusos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LostFoundService Tests")
class LostFoundServiceTest {

    @Mock
    private LostFoundRepository lostFoundRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LostFoundService lostFoundService;

    private User student1;
    private User student2;
    private User admin;
    private LostFoundItem testItem;

    @BeforeEach
    void setUp() {
        student1 = User.builder().id(1L).email("student1@campus.edu").name("Student One").role(Role.STUDENT).build();
        student2 = User.builder().id(2L).email("student2@campus.edu").name("Student Two").role(Role.STUDENT).build();
        admin = User.builder().id(3L).email("admin@campus.edu").name("Admin").role(Role.ADMIN).build();

        testItem = LostFoundItem.builder()
                .id(10L)
                .title("Blue Backpack")
                .description("Contains notebooks")
                .itemType("Bag")
                .listingType(ListingType.LOST)
                .location("Library 2nd Floor")
                .dateOccurred(LocalDate.now().minusDays(1))
                .status(LostFoundStatus.OPEN)
                .reportedBy(student1)
                .build();
    }

    private void mockAuth(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("createListing — successfully creates item with OPEN status")
    void testCreateListing_Success() {
        mockAuth(student1);

        CreateLostFoundRequest req = CreateLostFoundRequest.builder()
                .title("Keys")
                .description("Key bunch with blue tag")
                .itemType("Keys")
                .listingType(ListingType.FOUND)
                .location("Cafeteria")
                .dateOccurred(LocalDate.now())
                .build();

        when(lostFoundRepository.save(any(LostFoundItem.class))).thenAnswer(i -> {
            LostFoundItem item = i.getArgument(0);
            item.setId(20L);
            return item;
        });

        LostFoundResponse resp = lostFoundService.createListing(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getTitle()).isEqualTo("Keys");
        assertThat(resp.getStatus()).isEqualTo(LostFoundStatus.OPEN);
        assertThat(resp.getListingType()).isEqualTo(ListingType.FOUND);
        assertThat(resp.getReportedBy().getEmail()).isEqualTo("student1@campus.edu");
    }

    @Test
    @DisplayName("createListing — rejects future dateOccurred")
    void testCreateListing_FutureDate_ThrowsBadRequest() {
        mockAuth(student1);

        CreateLostFoundRequest req = CreateLostFoundRequest.builder()
                .title("Keys")
                .itemType("Keys")
                .listingType(ListingType.FOUND)
                .location("Cafeteria")
                .dateOccurred(LocalDate.now().plusDays(2))
                .build();

        assertThatThrownBy(() -> lostFoundService.createListing(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("updateListing — owner can update successfully")
    void testUpdateListing_OwnerSuccess() {
        mockAuth(student1);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));
        when(lostFoundRepository.save(any(LostFoundItem.class))).thenReturn(testItem);

        UpdateLostFoundRequest req = UpdateLostFoundRequest.builder()
                .title("Blue Backpack Updated")
                .build();

        LostFoundResponse resp = lostFoundService.updateListing(10L, req);
        assertThat(resp.getTitle()).isEqualTo("Blue Backpack Updated");
    }

    @Test
    @DisplayName("updateListing — non-owner cannot update (403 Unauthorized)")
    void testUpdateListing_NonOwner_ThrowsUnauthorized() {
        mockAuth(student2);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));

        UpdateLostFoundRequest req = UpdateLostFoundRequest.builder()
                .title("Hacked Title")
                .build();

        assertThatThrownBy(() -> lostFoundService.updateListing(10L, req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("deleteListing — admin can delete any listing")
    void testDeleteListing_AdminSuccess() {
        mockAuth(admin);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));

        lostFoundService.deleteListing(10L);
        verify(lostFoundRepository, times(1)).delete(testItem);
    }

    @Test
    @DisplayName("updateStatus — valid status transition OPEN -> MATCHED")
    void testUpdateStatus_ValidOpenToMatched() {
        mockAuth(student1);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));
        when(lostFoundRepository.save(any(LostFoundItem.class))).thenAnswer(i -> i.getArgument(0));

        UpdateLostFoundStatusRequest req = new UpdateLostFoundStatusRequest(LostFoundStatus.MATCHED);
        LostFoundResponse resp = lostFoundService.updateStatus(10L, req);

        assertThat(resp.getStatus()).isEqualTo(LostFoundStatus.MATCHED);
    }

    @Test
    @DisplayName("updateStatus — valid status transition MATCHED -> RECOVERED")
    void testUpdateStatus_ValidMatchedToRecovered() {
        testItem.setStatus(LostFoundStatus.MATCHED);
        mockAuth(student1);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));
        when(lostFoundRepository.save(any(LostFoundItem.class))).thenAnswer(i -> i.getArgument(0));

        UpdateLostFoundStatusRequest req = new UpdateLostFoundStatusRequest(LostFoundStatus.RECOVERED);
        LostFoundResponse resp = lostFoundService.updateStatus(10L, req);

        assertThat(resp.getStatus()).isEqualTo(LostFoundStatus.RECOVERED);
    }

    @Test
    @DisplayName("updateStatus — invalid status transition OPEN -> RECOVERED throws BadRequest")
    void testUpdateStatus_InvalidOpenToRecovered_ThrowsBadRequest() {
        mockAuth(student1);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));

        UpdateLostFoundStatusRequest req = new UpdateLostFoundStatusRequest(LostFoundStatus.RECOVERED);
        assertThatThrownBy(() -> lostFoundService.updateStatus(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("updateStatus — closed item cannot transition to anything")
    void testUpdateStatus_ClosedToOpen_ThrowsBadRequest() {
        testItem.setStatus(LostFoundStatus.CLOSED);
        mockAuth(student1);
        when(lostFoundRepository.findById(10L)).thenReturn(Optional.of(testItem));

        UpdateLostFoundStatusRequest req = new UpdateLostFoundStatusRequest(LostFoundStatus.OPEN);
        assertThatThrownBy(() -> lostFoundService.updateStatus(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("getAllListings — returns paginated results")
    void testGetAllListings_Success() {
        Page<LostFoundItem> page = new PageImpl<>(List.of(testItem));
        when(lostFoundRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<LostFoundResponse> resp = lostFoundService.getAllListings(null, null, "backpack", PageRequest.of(0, 10));

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getTitle()).isEqualTo("Blue Backpack");
    }
}
