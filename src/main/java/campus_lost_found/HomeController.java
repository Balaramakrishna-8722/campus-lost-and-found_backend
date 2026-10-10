package campus_lost_found;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import campus_lost_found.model.Claim;
import campus_lost_found.model.ClaimRequest;
import campus_lost_found.model.Item;
import campus_lost_found.repository.ClaimRepository;
import campus_lost_found.repository.ItemRepository;

@Controller
public class HomeController {

    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final ClaimMatchingService claimMatchingService;

    public HomeController(
            ItemRepository itemRepository,
            ClaimRepository claimRepository,
            ClaimMatchingService claimMatchingService) {

        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.claimMatchingService = claimMatchingService;
    }

    // =========================================================
    // HOME
    // =========================================================

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // =========================================================
    // LOST ITEM
    // =========================================================

    @GetMapping("/lost")
    public String lostPage() {
        return "lost";
    }

    @PostMapping("/lost")
    public String submitLostItem(
            @RequestParam String itemName,
            @RequestParam String location,
            @RequestParam String description,
            @RequestParam String contact,
            Model model) {

        Item item = new Item();

        item.setItemType("LOST");
        item.setItemName(itemName);
        item.setLocation(location);
        item.setDescription(description);
        item.setContact(contact);
        item.setItemStatus("AVAILABLE");

        itemRepository.save(item);

        model.addAttribute("itemName", itemName);
        model.addAttribute("location", location);
        model.addAttribute("description", description);

        return "lost-success";
    }

    // =========================================================
    // FOUND ITEM
    // =========================================================

    @GetMapping("/found")
    public String foundPage() {
        return "found";
    }

    @PostMapping("/found")
    public String submitFoundItem(
            @RequestParam String itemName,
            @RequestParam String location,
            @RequestParam String description,
            @RequestParam String contact,
            Model model) {

        Item item = new Item();

        item.setItemType("FOUND");
        item.setItemName(itemName);
        item.setLocation(location);
        item.setDescription(description);
        item.setContact(contact);
        item.setItemStatus("AVAILABLE");

        itemRepository.save(item);

        model.addAttribute("itemName", itemName);
        model.addAttribute("location", location);
        model.addAttribute("description", description);

        return "found-success";
    }

    // =========================================================
    // REPORTS
    // =========================================================

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            Model model) {

        List<Item> items;

        if (search != null && !search.trim().isEmpty()) {

            String searchText = search.trim();

            if ("LOST".equalsIgnoreCase(type)
                    || "FOUND".equalsIgnoreCase(type)) {

                items = itemRepository
                        .findByItemTypeAndItemNameContainingIgnoreCaseOrItemTypeAndLocationContainingIgnoreCase(
                                type.toUpperCase(),
                                searchText,
                                type.toUpperCase(),
                                searchText);

            } else {

                items = itemRepository
                        .findByItemNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
                                searchText,
                                searchText);
            }

        } else if ("LOST".equalsIgnoreCase(type)
                || "FOUND".equalsIgnoreCase(type)) {

            items = itemRepository.findByItemType(type.toUpperCase());

        } else {

            items = itemRepository.findAll();
        }

        model.addAttribute("items", items);
        model.addAttribute("search", search);
        model.addAttribute("type", type);

        return "reports";
    }

    // =========================================================
    // REPORTS API
    // =========================================================

    @GetMapping("/api/reports")
    public ResponseEntity<List<Item>> getReports() {

        List<Item> items = itemRepository.findAll();

        return ResponseEntity.ok(items);
    }

    // =========================================================
    // CLAIM PAGE
    // =========================================================

    @GetMapping("/claim/{id}")
    public String claimPage(
            @PathVariable Long id,
            Model model) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Item not found"));

        if (!"FOUND".equalsIgnoreCase(item.getItemType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only found items can be claimed");
        }

        if ("CLAIMED".equalsIgnoreCase(item.getItemStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This item has already been claimed");
        }

        model.addAttribute("item", item);

        return "claim";
    }

    // =========================================================
    // CLAIM SUBMISSION - WEBSITE
    // =========================================================

    @PostMapping("/claim/{id}")
    public String submitClaim(
            @PathVariable Long id,
            @RequestParam String claimantName,
            @RequestParam String claimantContact,
            @RequestParam String lostLocation,
            @RequestParam String lostTime,
            @RequestParam String identifyingDetails,
            Model model) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Item not found"));

        if (!"FOUND".equalsIgnoreCase(item.getItemType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only found items can be claimed");
        }

        if ("CLAIMED".equalsIgnoreCase(item.getItemStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This item has already been claimed");
        }

        Claim claim = new Claim();

        claim.setItemId(item.getId());
        claim.setClaimantName(claimantName);
        claim.setClaimantContact(claimantContact);
        claim.setLostLocation(lostLocation);
        claim.setLostTime(lostTime);
        claim.setIdentifyingDetails(identifyingDetails);
        claim.setStatus("PENDING");

        // =====================================================
        // CLAIM MATCHING
        // =====================================================

        ClaimMatchingService.MatchResult matchResult =
                claimMatchingService.analyze(item, claim);

        claim.setMatchScore(matchResult.getScore());
        claim.setMatchRecommendation(
                matchResult.getRecommendation());
        claim.setMatchReason(
                matchResult.getReasons());

        claimRepository.save(claim);

        // =====================================================
        // CLAIM SUCCESS PAGE
        // =====================================================

        model.addAttribute("item", item);
        model.addAttribute("claimantName", claimantName);
        model.addAttribute("claimantContact", claimantContact);
        model.addAttribute("lostLocation", lostLocation);
        model.addAttribute("lostTime", lostTime);
        model.addAttribute(
                "identifyingDetails",
                identifyingDetails);

        return "claim-success";
    }

    // =========================================================
    // CLAIM API - POSTMAN
    // =========================================================

    @PostMapping("/api/claims/{id}")
    public ResponseEntity<?> createClaim(
            @PathVariable Long id,
            @RequestBody ClaimRequest request) {

        Item item =
                itemRepository.findById(id)
                        .orElse(null);

        if (item == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Item not found");
        }

        if (!"FOUND".equalsIgnoreCase(item.getItemType())) {

            return ResponseEntity
                    .badRequest()
                    .body("Only found items can be claimed");
        }

        if ("CLAIMED".equalsIgnoreCase(item.getItemStatus())) {

            return ResponseEntity
                    .badRequest()
                    .body("This item has already been claimed");
        }

        Claim claim = new Claim();

        claim.setItemId(item.getId());

        claim.setClaimantName(
                request.getClaimantName());

        claim.setClaimantContact(
                request.getClaimantContact());

        claim.setLostLocation(
                request.getLostLocation());

        claim.setLostTime(
                request.getLostTime());

        claim.setIdentifyingDetails(
                request.getIdentifyingDetails());

        claim.setStatus("PENDING");

        // =====================================================
        // CLAIM MATCHING
        // =====================================================

        ClaimMatchingService.MatchResult matchResult =
                claimMatchingService.analyze(item, claim);

        claim.setMatchScore(
                matchResult.getScore());

        claim.setMatchRecommendation(
                matchResult.getRecommendation());

        claim.setMatchReason(
                matchResult.getReasons());

        Claim savedClaim =
                claimRepository.save(claim);

        return ResponseEntity.ok(savedClaim);
    }

    // =========================================================
    // ALL CLAIMS
    // =========================================================

    @GetMapping("/claims")
    public String claims(Model model) {

        List<Claim> claims =
                claimRepository.findAll();

        model.addAttribute(
                "claims",
                claims);

        return "claims";
    }

    // =========================================================
    // CLAIMS API
    // =========================================================

    @GetMapping("/api/claims")
    public ResponseEntity<List<Claim>> getClaims() {

        List<Claim> claims =
                claimRepository.findAll();

        return ResponseEntity.ok(claims);
    }
}