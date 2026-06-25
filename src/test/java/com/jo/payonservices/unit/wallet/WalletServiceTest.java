package com.jo.payonservices.unit.wallet;

import com.jo.payonservices.auth.dto.AuthApiResponse;
import com.jo.payonservices.auth.model.users;
import com.jo.payonservices.auth.repository.UserRepo;
import com.jo.payonservices.wallet.dto.WalletResponse;
import com.jo.payonservices.wallet.model.Wallet;
import com.jo.payonservices.wallet.repository.WalletRepo;
import com.jo.payonservices.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock private WalletRepo walletRepo;
    @Mock private UserRepo userRepo;
    @Mock private Authentication auth;

    @InjectMocks
    private WalletService walletService;

    private users mockUser;
    private UUID mockUserId;

    @BeforeEach
    void setUp() {
        mockUserId = UUID.randomUUID();
        mockUser = users.builder()
                .userId(mockUserId)
                .userName("yousef")
                .email("yousef@test.com")
                .build();
    }

    @Test
    @DisplayName("Initialize Wallet - Success Scenario")
    void initializeWallet_ShouldCreateWalletSuccessfully() {
        when(auth.getPrincipal()).thenReturn("yousef");
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUser_UserId(mockUserId)).thenReturn(Optional.empty());

        WalletResponse response = walletService.initializeWallet(auth);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertEquals(mockUserId.toString(), response.getUserId());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepo, times(1)).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertEquals(mockUser, savedWallet.getUser());
        assertEquals(BigDecimal.ZERO, savedWallet.getBalance());
    }

    @Test
    @DisplayName("Initialize Wallet - Throws Conflict When Wallet Exists")
    void initializeWallet_ShouldThrowConflict_WhenWalletAlreadyExists() {
        when(auth.getPrincipal()).thenReturn("yousef");
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUser_UserId(mockUserId)).thenReturn(Optional.of(new Wallet()));

        assertThrows(ResponseStatusException.class, () -> walletService.initializeWallet(auth));
        verify(walletRepo, never()).save(any(Wallet.class));
    }



    @Test
    @DisplayName("Get Wallet - Returns Wallet Response Successfully")
    void getWallet_ShouldReturnWallet_WhenUserExists() {
        UUID walletId = UUID.randomUUID();
        Wallet mockWallet = new Wallet();
        mockWallet.setWalletId(walletId);
        mockWallet.setUser(mockUser);
        mockWallet.setBalance(BigDecimal.TEN);

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("yousef");
        when(userRepo.findByUserName("yousef")).thenReturn(Optional.of(mockUser));
        when(walletRepo.findByUser_UserId(mockUserId)).thenReturn(Optional.of(mockWallet));


        WalletResponse response = walletService.getWallet(auth);

        assertNotNull(response);
        assertEquals(walletId, response.getWalletId());
        assertEquals(mockUserId.toString(), response.getUserId());
        assertEquals(BigDecimal.TEN, response.getBalance());
    }

    @Test
    @DisplayName("Get Wallet - Throws 404 When User Not Found")
    void getWallet_ShouldThrowException_WhenUserNotFound() {
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("unknown_user");
        when(userRepo.findByUserName("unknown_user")).thenReturn(Optional.empty()); // السيرفيس ستضرب هنا فوراً

        assertThrows(ResponseStatusException.class, () -> walletService.getWallet(auth));
    }

    @Test
    @DisplayName("Get Wallet - Throws 401 When Not Authenticated")
    void getWallet_ShouldThrowException_WhenNotAuthenticated() {
        when(auth.isAuthenticated()).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> walletService.getWallet(auth));
    }
    @Test
    @DisplayName("SyncContacts - Returns List of Matched Users")
    void syncContacts_Success() {
        List<String> phoneNumbers = List.of("123456", "654321");
        users user1 = users.builder().userName("yousef").phoneNumber("123456").thumbnailPath("/path/1").build();
        users user2 = users.builder().userName("omar").phoneNumber("654321").thumbnailPath("/path/2").build();

        when(userRepo.findByPhoneNumberIn(phoneNumbers)).thenReturn(List.of(user1, user2));

        AuthApiResponse<?> response = walletService.syncContacts(phoneNumbers);

        assertNotNull(response);
        assertEquals(2, ((List<?>) response.getData()).size());
        verify(userRepo).findByPhoneNumberIn(phoneNumbers);
    }
}