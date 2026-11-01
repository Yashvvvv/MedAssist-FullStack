package com.medassist.sales;

import com.medassist.common.time.BusinessTime;
import com.medassist.tenancy.Branch;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document numbers: {@code <BRANCH>/<FY>/<SEQ>}, e.g. {@code MAIN/2627/0042}. GST rules
 * want invoice numbers unique per fiscal year and at most 16 characters, so the branch
 * code is cut to 6 and the year written as 4 digits. Credit notes use a separate series
 * with a C prefix, online orders an O prefix.
 */
@Component
public class DocumentNumbers {

    public enum Type { INV, CN, ORD }

    private final SaleRepository sales;
    private final BusinessTime time;

    public DocumentNumbers(SaleRepository sales, BusinessTime time) {
        this.sales = sales;
        this.time = time;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(Branch branch, Type type) {
        String fy = time.fiscalYear(time.today());
        long seq = sales.nextSequence(branch.getId(), fy, type.name());
        String code = branch.getCode().length() > 6 ? branch.getCode().substring(0, 6) : branch.getCode();
        String prefix = switch (type) {
            case INV -> "";
            case CN -> "C";
            case ORD -> "O";
        };
        return prefix + code + "/" + fy.replace("-", "") + "/" + String.format("%04d", seq);
    }
}
