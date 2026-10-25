package service;

import model.Booking.Quote;
import model.RoomSlot;
import util.AppConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure calculation engine for slot selection validation and quotation pricing.
 * Does not depend on database or servlet APIs.
 */
public final class PricingService {

    public static final String MSG_NON_CONSECUTIVE = "Please select consecutive time slots.";

    /**
     * Discount percentages for 2, 3, and 4 consecutive slots.
     */
    public static final class DiscountRates {
        public final int twoSlots;
        public final int threeSlots;
        public final int fourSlots;

        public DiscountRates(int two, int three, int four) {
            this.twoSlots = two;
            this.threeSlots = three;
            this.fourSlots = four;
        }

        public int getTwoSlots() {
            return twoSlots;
        }

        public int getThreeSlots() {
            return threeSlots;
        }

        public int getFourSlots() {
            return fourSlots;
        }

        public static DiscountRates fromConfig() {
            int two = AppConstants.DISCOUNT_2_SLOTS_PERCENT;
            int three = AppConstants.DISCOUNT_3_SLOTS_PERCENT;
            int four = AppConstants.DISCOUNT_4_SLOTS_PERCENT;
            return new DiscountRates(two, three, four);
        }
    }

    public PricingService() {
    }

    /**
     * Calculates a quote for the selected slot codes according to business discount rules.
     *
     * @param roomSlots     all slots of the room, as loaded from ROOM_TIME_SLOT
     * @param selectedCodes slot codes chosen by client (e.g. ["S1", "S2"])
     * @param rates         discount rate configuration
     * @return calculated Quote
     * @throws ServiceException if the selection is invalid, duplicate, or non-consecutive
     */
    public Quote quote(List<RoomSlot> roomSlots, Collection<String> selectedCodes, DiscountRates rates)
            throws ServiceException {

        if (selectedCodes == null || selectedCodes.isEmpty()) {
            throw new ServiceException(ServiceException.EMPTY_SELECTION, "Please select at least one time slot.");
        }

        List<String> normalized = new ArrayList<>();
        Set<String> uniqueCodes = new HashSet<>();

        for (String c : selectedCodes) {
            if (c == null || c.trim().isEmpty()) {
                continue;
            }
            String norm = c.trim().toUpperCase();
            if (!uniqueCodes.add(norm)) {
                throw new ServiceException(ServiceException.DUPLICATE_SLOT, "Each time slot can be selected only once.");
            }
            normalized.add(norm);
        }

        if (normalized.isEmpty()) {
            throw new ServiceException(ServiceException.EMPTY_SELECTION, "Please select at least one time slot.");
        }

        if (normalized.size() > 4) {
            throw new ServiceException(ServiceException.TOO_MANY_SLOTS, "You can select at most 4 time slots.");
        }

        Map<String, RoomSlot> slotMap = new HashMap<>();
        if (roomSlots != null) {
            for (RoomSlot rs : roomSlots) {
                if (rs != null && rs.isActive() && rs.getSlotCode() != null) {
                    slotMap.put(rs.getSlotCode().trim().toUpperCase(), rs);
                }
            }
        }

        List<RoomSlot> selectedSlots = new ArrayList<>();
        for (String code : normalized) {
            RoomSlot rs = slotMap.get(code);
            if (rs == null) {
                throw new ServiceException(ServiceException.UNKNOWN_SLOT,
                        "One or more selected time slots are not offered for this room.");
            }
            selectedSlots.add(rs);
        }

        Collections.sort(selectedSlots, new Comparator<RoomSlot>() {
            @Override
            public int compare(RoomSlot o1, RoomSlot o2) {
                return Integer.compare(o1.getSortOrder(), o2.getSortOrder());
            }
        });

        int minSort = selectedSlots.get(0).getSortOrder();
        int maxSort = selectedSlots.get(selectedSlots.size() - 1).getSortOrder();
        if ((maxSort - minSort + 1) != selectedSlots.size()) {
            throw new ServiceException(ServiceException.NON_CONSECUTIVE_SLOTS, MSG_NON_CONSECUTIVE);
        }

        long subtotal = 0L;
        for (RoomSlot rs : selectedSlots) {
            subtotal += rs.getListPrice();
        }

        int percent = 0;
        int count = selectedSlots.size();
        if (rates != null) {
            if (count == 2) {
                percent = rates.twoSlots;
            } else if (count == 3) {
                percent = rates.threeSlots;
            } else if (count == 4) {
                percent = rates.fourSlots;
            }
        }

        long discountAmount = 0L;
        if (percent > 0) {
            discountAmount = BigDecimal.valueOf(subtotal)
                    .multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact();
        }

        long total = subtotal - discountAmount;
        return new Quote(selectedSlots, subtotal, percent, discountAmount, total, count);
    }
}
