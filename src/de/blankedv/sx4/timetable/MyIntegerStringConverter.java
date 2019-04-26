/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4.timetable;

import static de.blankedv.sx4.Constants.INVALID_INT;
import javafx.util.converter.IntegerStringConverter;

/**
 *
 * @author mblank
 */
public class MyIntegerStringConverter extends IntegerStringConverter {
    @Override
            public Integer fromString(String s) {
                String digits = s.replaceAll("[^0-9]", "");
                int i = INVALID_INT;
                try {
                     i = Integer.parseInt(digits);
                } catch (NumberFormatException ex) {
                    
                }
                return i;
            }
}
