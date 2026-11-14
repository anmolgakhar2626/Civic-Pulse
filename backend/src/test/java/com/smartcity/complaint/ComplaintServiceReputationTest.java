package com.smartcity.complaint;

import com.smartcity.common.ForbiddenException;
import com.smartcity.complaint.dto.CitizenReputationResponse;
import com.smartcity.complaint.dto.ComplaintResponse;
import com.smartcity.complaint.dto.CreateComplaintRequest;
import com.smartcity.complaint.dto.VerifyComplaintRequest;
import com.smartcity.grievance.portal.GovernmentGrievanceService;
import com.smartcity.user.Role;
import com.smartcity.user.User;
import com.smartcity.user.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceReputationTest {

    @Mock
    private ComplaintDao complaintDao;

    @Mock
    private UserDao userDao;

    @Mock
    private GovernmentGrievanceService grievanceService;

    private ComplaintServiceImpl complaintService;

    private User citizen;
    private User officer;
    private User admin;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintServiceImpl(complaintDao, userDao, grievanceService);

        Role citizenRole = new Role();
        citizenRole.setName("ROLE_CITIZEN");

        Role officerRole = new Role();
        officerRole.setName("ROLE_DEPARTMENT_OFFICER");

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        citizen = new User();
        citizen.setId(UUID.randomUUID());
        citizen.setUsername("citizen_user");
        citizen.setFullName("Aarav Mehta");
        citizen.setMobileNumber("+919876543210");
        citizen.setIsMobileVerified(true);
        citizen.setRoles(Set.of(citizenRole));

        officer = new User();
        officer.setId(UUID.randomUUID());
        officer.setUsername("officer_traffic");
        officer.setFullName("Officer Vikram");
        officer.setRoles(Set.of(officerRole));

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin");
        admin.setFullName("Administrator");
        admin.setRoles(Set.of(adminRole));
    }

    @Test
    @DisplayName("Create Complaint: Unverified mobile number throws ForbiddenException")
    void createComplaint_unverifiedMobile_forbidden() {
        citizen.setIsMobileVerified(false);
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(citizen));

        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setCategory("ROADS");
        request.setDescription("Pothole on Main Street");

        assertThatThrownBy(() -> complaintService.createComplaint(request, citizen))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Mobile number verification is required");
    }

    @Test
    @DisplayName("Create Complaint: Verified citizen can file complaint with default UNVERIFIED status")
    void createComplaint_verifiedMobile_success() {
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(citizen));
        when(complaintDao.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Severe Pothole");
        request.setCategory("ROADS");
        request.setDescription("Severe pothole causing vehicle damage");

        ComplaintResponse response = complaintService.createComplaint(request, citizen);

        assertThat(response).isNotNull();
        assertThat(response.getComplaintVerificationStatus()).isEqualTo("UNVERIFIED");
        assertThat(response.getStatus()).isEqualTo("SUBMITTED");
        verify(complaintDao).save(any(Complaint.class));
    }

    @Test
    @DisplayName("Verify Complaint: Non-officer cannot verify complaints")
    void verifyComplaint_unauthorizedRole_forbidden() {
        UUID complaintId = UUID.randomUUID();
        VerifyComplaintRequest request = new VerifyComplaintRequest("GENUINE", "Inspected on site");

        assertThatThrownBy(() -> complaintService.verifyComplaint(complaintId, request, citizen))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only authorized Department Officers and Admins");
    }

    @Test
    @DisplayName("Verify Complaint: Officer cannot self-verify own complaints")
    void verifyComplaint_selfVerification_forbidden() {
        UUID complaintId = UUID.randomUUID();
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setCitizen(officer); // Officer is also the citizen who filed it

        when(complaintDao.findById(complaintId)).thenReturn(Optional.of(complaint));

        VerifyComplaintRequest request = new VerifyComplaintRequest("GENUINE", "Self inspection");

        assertThatThrownBy(() -> complaintService.verifyComplaint(complaintId, request, officer))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Conflict of interest: You cannot verify your own civic complaints");
    }

    @Test
    @DisplayName("Reputation: 4 genuine complaints does not award badge (threshold > 4, i.e., >= 5)")
    void verifyComplaint_fourGenuine_noBadgeAwarded() {
        UUID complaintId = UUID.randomUUID();
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setCitizen(citizen);
        complaint.setComplaintVerificationStatus("UNVERIFIED");

        when(complaintDao.findById(complaintId)).thenReturn(Optional.of(complaint));
        // Return 4 genuine complaints for citizen
        when(complaintDao.countGenuineComplaintsByCitizen(citizen.getId())).thenReturn(4);

        User updatedCitizen = new User();
        updatedCitizen.setId(citizen.getId());
        updatedCitizen.setGenuineComplaintCount(4);
        updatedCitizen.setIsVerifiedCitizen(false);
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(updatedCitizen));

        VerifyComplaintRequest request = new VerifyComplaintRequest("GENUINE", "Verified as genuine road hazard");
        ComplaintResponse response = complaintService.verifyComplaint(complaintId, request, officer);

        // Verify reputation update was called with false
        verify(userDao).updateReputation(citizen.getId(), 4, false);
        verify(complaintDao).saveVerificationHistory(any(ComplaintVerificationHistory.class));
        assertThat(response.isCitizenIsVerifiedBadge()).isFalse();
        assertThat(response.getCitizenGenuineCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Reputation: 5 genuine complaints awards Verified Citizen badge")
    void verifyComplaint_fiveGenuine_badgeAwarded() {
        UUID complaintId = UUID.randomUUID();
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setCitizen(citizen);
        complaint.setComplaintVerificationStatus("UNVERIFIED");

        when(complaintDao.findById(complaintId)).thenReturn(Optional.of(complaint));
        // 5 genuine complaints verified
        when(complaintDao.countGenuineComplaintsByCitizen(citizen.getId())).thenReturn(5);

        User updatedCitizen = new User();
        updatedCitizen.setId(citizen.getId());
        updatedCitizen.setGenuineComplaintCount(5);
        updatedCitizen.setIsVerifiedCitizen(true);
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(updatedCitizen));

        VerifyComplaintRequest request = new VerifyComplaintRequest("GENUINE", "Verified 5th genuine civic issue");
        ComplaintResponse response = complaintService.verifyComplaint(complaintId, request, officer);

        // Verify badge awarded
        verify(userDao).updateReputation(citizen.getId(), 5, true);
        assertThat(response.isCitizenIsVerifiedBadge()).isTrue();
        assertThat(response.getCitizenGenuineCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Reputation: Status reversal from GENUINE to REJECTED_SPAM revokes badge when count drops below 5")
    void verifyComplaint_statusReversal_revokesBadge() {
        UUID complaintId = UUID.randomUUID();
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setCitizen(citizen);
        complaint.setComplaintVerificationStatus("GENUINE");

        when(complaintDao.findById(complaintId)).thenReturn(Optional.of(complaint));
        // Now count drops back to 4
        when(complaintDao.countGenuineComplaintsByCitizen(citizen.getId())).thenReturn(4);

        User updatedCitizen = new User();
        updatedCitizen.setId(citizen.getId());
        updatedCitizen.setGenuineComplaintCount(4);
        updatedCitizen.setIsVerifiedCitizen(false);
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(updatedCitizen));

        VerifyComplaintRequest request = new VerifyComplaintRequest("REJECTED_SPAM", "Investigation revealed fabricated photo");
        ComplaintResponse response = complaintService.verifyComplaint(complaintId, request, officer);

        // Verify badge revoked
        verify(userDao).updateReputation(citizen.getId(), 4, false);
        assertThat(response.isCitizenIsVerifiedBadge()).isFalse();
        assertThat(response.getCitizenGenuineCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Reputation: Citizen reputation lookup provides accurate threshold details")
    void getCitizenReputation_success() {
        when(userDao.findById(citizen.getId())).thenReturn(Optional.of(citizen));
        when(complaintDao.countGenuineComplaintsByCitizen(citizen.getId())).thenReturn(3);

        CitizenReputationResponse rep = complaintService.getCitizenReputation(citizen.getId());

        assertThat(rep.getGenuineComplaintCount()).isEqualTo(3);
        assertThat(rep.isVerifiedCitizen()).isFalse();
        assertThat(rep.getThresholdRequired()).isEqualTo(5);
        assertThat(rep.getComplaintsNeededForBadge()).isEqualTo(2);
    }
}
