package campus_lost_found;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import campus_lost_found.model.Admin;
import campus_lost_found.model.Claim;
import campus_lost_found.model.Item;
import campus_lost_found.repository.AdminRepository;
import campus_lost_found.repository.ClaimRepository;
import campus_lost_found.repository.ItemRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class AdminController {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final AdminRepository adminRepository;

    public AdminController(
            ClaimRepository claimRepository,
            ItemRepository itemRepository,
            AdminRepository adminRepository) {

        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.adminRepository = adminRepository;
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        Optional<Admin> admin = adminRepository.findByUsername(username);

        if (admin.isPresent() && admin.get().getPassword().equals(password)) {

            session.setAttribute(
                    "adminLoggedIn",
                    true
            );

            return "redirect:/admin/dashboard";
        }

        model.addAttribute(
                "error",
                "Invalid username or password"
        );

        return "admin-login";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(
            HttpSession session,
            Model model) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        List<Claim> claims =
                claimRepository.findAll();

        List<Item> items =
                itemRepository.findAll();

        /*
         * If an item has already been handed over,
         * no other pending or approved claim for
         * that same item should remain active.
         *
         * This also cleans up claims that were created
         * before the item lifecycle protection was added.
         */
        for (Claim claim : claims) {

            Item item = itemRepository
                    .findById(claim.getItemId())
                    .orElse(null);

            if (item == null) {
                continue;
            }

            if ("CLAIMED".equals(item.getItemStatus())
                    && ("PENDING".equals(claim.getStatus())
                    || "APPROVED".equals(claim.getStatus()))) {

                claim.setStatus("REJECTED");

                claimRepository.save(claim);
            }
        }

        /*
         * Reload claims after any status corrections.
         */
        claims = claimRepository.findAll();

        model.addAttribute("claims", claims);
        model.addAttribute("items", items);

        return "admin-dashboard";
    }

    /*
     * ADMIN APPROVES A CLAIM
     *
     * The item must still be AVAILABLE.
     *
     * If another approved claim has already resulted
     * in the item being handed over, this claim cannot
     * be approved.
     */
    @GetMapping("/admin/claims/approve/{id}")
    public String approveClaim(
            @PathVariable Long id,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        Claim claim = claimRepository
                .findById(id)
                .orElse(null);

        if (claim == null) {
            return "redirect:/admin/dashboard";
        }

        /*
         * Only pending claims can be approved.
         */
        if (!"PENDING".equals(claim.getStatus())) {
            return "redirect:/admin/dashboard";
        }

        Item item = itemRepository
                .findById(claim.getItemId())
                .orElse(null);

        if (item == null) {
            return "redirect:/admin/dashboard";
        }

        /*
         * Do not approve a claim if the item has
         * already been handed over to its owner.
         */
        if ("CLAIMED".equals(item.getItemStatus())) {

            claim.setStatus("REJECTED");

            claimRepository.save(claim);

            return "redirect:/admin/dashboard";
        }

        claim.setStatus("APPROVED");

        claimRepository.save(claim);

        return "redirect:/admin/dashboard";
    }

    /*
     * ADMIN REJECTS A CLAIM
     */
    @GetMapping("/admin/claims/reject/{id}")
    public String rejectClaim(
            @PathVariable Long id,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        Claim claim = claimRepository
                .findById(id)
                .orElse(null);

        if (claim != null) {

            /*
             * Only pending claims can be rejected.
             */
            if ("PENDING".equals(claim.getStatus())) {

                claim.setStatus("REJECTED");

                claimRepository.save(claim);
            }
        }

        return "redirect:/admin/dashboard";
    }

    /*
     * MARK ITEM AS CLAIMED
     *
     * This is used AFTER the owner actually
     * receives the item.
     *
     * Only an APPROVED claim can be completed.
     */
    @GetMapping("/admin/claims/complete/{id}")
    public String completeClaim(
            @PathVariable Long id,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        Claim claim = claimRepository
                .findById(id)
                .orElse(null);

        if (claim == null) {
            return "redirect:/admin/dashboard";
        }

        /*
         * Only an approved claim can become completed.
         */
        if (!"APPROVED".equals(claim.getStatus())) {
            return "redirect:/admin/dashboard";
        }

        Item item = itemRepository
                .findById(claim.getItemId())
                .orElse(null);

        if (item == null) {
            return "redirect:/admin/dashboard";
        }

        /*
         * Do not allow another claim to complete
         * an item that has already been handed over.
         */
        if ("CLAIMED".equals(item.getItemStatus())) {

            claim.setStatus("REJECTED");

            claimRepository.save(claim);

            return "redirect:/admin/dashboard";
        }

        /*
         * Mark the selected claim as completed.
         */
        claim.setStatus("COMPLETED");

        claimRepository.save(claim);

        /*
         * Mark the item as no longer available.
         *
         * This prevents new claims.
         */
        item.setItemStatus("CLAIMED");

        itemRepository.save(item);

        /*
         * Any other pending or approved claims
         * for the same item are no longer valid
         * because the owner has already received it.
         */
        List<Claim> allClaims =
                claimRepository.findAll();

        for (Claim otherClaim : allClaims) {

            if (otherClaim.getId().equals(claim.getId())) {
                continue;
            }

            if (!item.getId().equals(otherClaim.getItemId())) {
                continue;
            }

            if ("PENDING".equals(otherClaim.getStatus())
                    || "APPROVED".equals(otherClaim.getStatus())) {

                otherClaim.setStatus("REJECTED");

                claimRepository.save(otherClaim);
            }
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/admin/login";
    }

    private boolean isAdminLoggedIn(
            HttpSession session) {

        Object loggedIn =
                session.getAttribute("adminLoggedIn");

        return Boolean.TRUE.equals(loggedIn);
    }
}