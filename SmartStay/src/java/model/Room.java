package model;

import java.io.Serializable;

/**
 * Model representing a ROOM record.
 */
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final java.util.Set<String> ALLOWED_STATUSES = java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList("AVAILABLE", "OCCUPIED", "NEEDS_CLEANING", "MAINTENANCE"))
    );

    private long roomId;
    private String roomNumber;
    private String displayName;
    private int floorNumber;
    private String locationDescription;
    private int capacity;
    private String description;
    private String amenities;
    private String imageUrl;
    private String status;
    private boolean active;
    private Long minPrice;
    private Long overnightPrice;

    public Room() {
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayTitle() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        return (roomNumber != null && !roomNumber.trim().isEmpty()) ? ("Room " + roomNumber) : "Room";
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getResolvedImageUrl() {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String u = imageUrl.trim();
            return u.startsWith("/") ? u.substring(1) : u;
        }
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            java.util.List<util.ElFunctions.GalleryImage> gallery =
                    util.ElFunctions.getGalleryImages(roomNumber, displayName);
            if (!gallery.isEmpty()) {
                return gallery.get(0).getUrl();
            }
        }
        return "assets/images/branding/hero-bg.jpg";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Long minPrice) {
        this.minPrice = minPrice;
    }

    public Long getOvernightPrice() {
        if (overnightPrice != null && overnightPrice > 0) {
            return overnightPrice;
        }
        if (minPrice != null && minPrice > 0) {
            return minPrice + 100000L;
        }
        return null;
    }

    public void setOvernightPrice(Long overnightPrice) {
        this.overnightPrice = overnightPrice;
    }

    public String getCaption() {
        if (displayName != null) {
            String name = displayName.trim().toLowerCase();
            if (name.contains("butter")) {
                return "Warm, creamy tones with a cozy kitchen and plush king bed designed for restful relaxation.";
            } else if (name.contains("vanilla") || name.contains("zen")) {
                return "Minimalist peaceful sanctuary with gentle ambience, soothing light, and natural harmony.";
            } else if (name.contains("fresh") || name.contains("tale")) {
                return "Refreshing airy suite equipped with deep soaking bathtub, cinema projector, and sofa bed.";
            } else if (name.contains("power") || name.contains("rush")) {
                return "Vibrant modern living space with energetic aesthetic, projector, and ergonomic lounge.";
            } else if (name.contains("sunny") || name.contains("love")) {
                return "Bright, romantic aesthetic filled with warmth, cinema projector, and vanity glow.";
            } else if (name.contains("warm") || name.contains("house")) {
                return "Inviting and playful atmosphere complete with mini game console, plush bean bag, and cozy retreat.";
            }
        }
        return (description != null && !description.trim().isEmpty())
                ? description : "Boutique curated art space with automated smart self check-in.";
    }

    public String getTag() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.toUpperCase();
        }
        return (roomNumber != null) ? roomNumber.toUpperCase() : "SWEET SPACE";
    }

    private java.util.List<RoomSlot> slots = new java.util.ArrayList<>();

    public java.util.List<RoomSlot> getSlots() {
        if (slots == null) {
            slots = new java.util.ArrayList<>();
        }
        return slots;
    }

    public void setSlots(java.util.List<RoomSlot> slots) {
        this.slots = slots;
    }

    // =========================================================================
    // INNER DTO: RoomCard (merged into Room)
    // =========================================================================

    /**
     * Model representing a room overview card with slot availabilities for the catalog and search pages.
     */
    public static class RoomCard implements Serializable {

        private static final long serialVersionUID = 1L;

        private Room room;
        private java.util.List<RoomSlot.SlotAvailability> slots;
        private long minPrice;
        private boolean bookable;

        public RoomCard() {
            this.slots = java.util.Collections.emptyList();
        }

        public RoomCard(Room room, java.util.List<RoomSlot.SlotAvailability> slots, long minPrice, boolean bookable) {
            this.room = room;
            this.slots = slots != null ? slots : java.util.Collections.<RoomSlot.SlotAvailability>emptyList();
            this.minPrice = minPrice;
            this.bookable = bookable;
        }

        public Room getRoom() {
            return room;
        }

        public void setRoom(Room room) {
            this.room = room;
        }

        public java.util.List<RoomSlot.SlotAvailability> getSlots() {
            return slots;
        }

        public void setSlots(java.util.List<RoomSlot.SlotAvailability> slots) {
            this.slots = slots != null ? slots : java.util.Collections.<RoomSlot.SlotAvailability>emptyList();
        }

        public long getMinPrice() {
            return minPrice;
        }

        public void setMinPrice(long minPrice) {
            this.minPrice = minPrice;
        }

        public boolean isBookable() {
            return bookable;
        }

        public void setBookable(boolean bookable) {
            this.bookable = bookable;
        }
    }
}
