package com.example.skillsplugin.skills;

import com.example.skillsplugin.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LevelFormula class.
 * Tests level progression calculations with various configurations.
 */
public class LevelFormulaTest {
    
    private LevelFormula defaultFormula;
    private ConfigManager mockConfig;
    
    @Before
    public void setUp() {
        defaultFormula = LevelFormula.createDefault();
        mockConfig = mock(ConfigManager.class);
    }
    
    @Test
    public void testDefaultFormulaLevel1() {
        // Level 1: 100 * (1 ^ 1.25) = 100
        double required = defaultFormula.getRequiredExperience(1);
        assertEquals("Level 1 should require 100 XP", 100.0, required, 0.001);
    }
    
    @Test
    public void testDefaultFormulaLevel2() {
        // Level 2: 100 * (2 ^ 1.25) = 237.84
        double required = defaultFormula.getRequiredExperience(2);
        assertEquals("Level 2 should require ~237.84 XP", 237.84, required, 0.01);
    }
    
    @Test
    public void testDefaultFormulaLevel5() {
        // Level 5: 100 * (5 ^ 1.25) = 843.90
        double required = defaultFormula.getRequiredExperience(5);
        assertEquals("Level 5 should require ~843.90 XP", 843.90, required, 0.01);
    }
    
    @Test
    public void testDefaultFormulaLevel10() {
        // Level 10: 100 * (10 ^ 1.25) = 2371.37
        double required = defaultFormula.getRequiredExperience(10);
        assertEquals("Level 10 should require ~2371.37 XP", 2371.37, required, 0.01);
    }
    
    @Test
    public void testDefaultFormulaLevel50() {
        // Level 50: 100 * (50 ^ 1.25) = 26265.28
        double required = defaultFormula.getRequiredExperience(50);
        assertEquals("Level 50 should require ~26265.28 XP", 26265.28, required, 0.01);
    }
    
    @Test
    public void testFormulaWithInvalidLevel() {
        double required = defaultFormula.getRequiredExperience(0);
        assertEquals("Level 0 should require 0 XP", 0.0, required, 0.001);
        
        required = defaultFormula.getRequiredExperience(-5);
        assertEquals("Negative level should require 0 XP", 0.0, required, 0.001);
    }
    
    @Test
    public void testFormulaScaling() {
        // Test that higher levels require progressively more XP
        double level1 = defaultFormula.getRequiredExperience(1);
        double level2 = defaultFormula.getRequiredExperience(2);
        double level3 = defaultFormula.getRequiredExperience(3);
        double level4 = defaultFormula.getRequiredExperience(4);
        
        assertTrue("Level 2 should require more than level 1", level2 > level1);
        assertTrue("Level 3 should require more than level 2", level3 > level2);
        assertTrue("Level 4 should require more than level 3", level4 > level3);
        
        // Test that the increase accelerates (exponential growth)
        double increase1to2 = level2 - level1;
        double increase2to3 = level3 - level2;
        double increase3to4 = level4 - level3;
        
        assertTrue("XP increase should accelerate", increase2to3 > increase1to2);
        assertTrue("XP increase should continue to accelerate", increase3to4 > increase2to3);
    }
    
    @Test
    public void testCustomConfigFormula() {
        // Mock config with custom values
        when(mockConfig.getBaseXP()).thenReturn(200.0);
        when(mockConfig.getExponent()).thenReturn(2.0);
        
        LevelFormula customFormula = new LevelFormula(mockConfig);
        
        // Level 1: 200 * (1 ^ 2.0) = 200
        double level1 = customFormula.getRequiredExperience(1);
        assertEquals("Custom formula level 1 should require 200 XP", 200.0, level1, 0.001);
        
        // Level 2: 200 * (2 ^ 2.0) = 800
        double level2 = customFormula.getRequiredExperience(2);
        assertEquals("Custom formula level 2 should require 800 XP", 800.0, level2, 0.001);
        
        // Level 5: 200 * (5 ^ 2.0) = 5000
        double level5 = customFormula.getRequiredExperience(5);
        assertEquals("Custom formula level 5 should require 5000 XP", 5000.0, level5, 0.001);
    }
    
    @Test
    public void testLinearFormula() {
        // Test with exponent = 1.0 for linear progression
        when(mockConfig.getBaseXP()).thenReturn(100.0);
        when(mockConfig.getExponent()).thenReturn(1.0);
        
        LevelFormula linearFormula = new LevelFormula(mockConfig);
        
        // Level 1: 100 * (1 ^ 1.0) = 100
        assertEquals("Linear level 1", 100.0, linearFormula.getRequiredExperience(1), 0.001);
        
        // Level 2: 100 * (2 ^ 1.0) = 200
        assertEquals("Linear level 2", 200.0, linearFormula.getRequiredExperience(2), 0.001);
        
        // Level 5: 100 * (5 ^ 1.0) = 500
        assertEquals("Linear level 5", 500.0, linearFormula.getRequiredExperience(5), 0.001);
    }
    
    @Test
    public void testSteepFormula() {
        // Test with high exponent for steep progression
        when(mockConfig.getBaseXP()).thenReturn(50.0);
        when(mockConfig.getExponent()).thenReturn(3.0);
        
        LevelFormula steepFormula = new LevelFormula(mockConfig);
        
        // Level 1: 50 * (1 ^ 3.0) = 50
        assertEquals("Steep level 1", 50.0, steepFormula.getRequiredExperience(1), 0.001);
        
        // Level 2: 50 * (2 ^ 3.0) = 400
        assertEquals("Steep level 2", 400.0, steepFormula.getRequiredExperience(2), 0.001);
        
        // Level 5: 50 * (5 ^ 3.0) = 6250
        assertEquals("Steep level 5", 6250.0, steepFormula.getRequiredExperience(5), 0.001);
    }
    
    @Test
    public void testLowBaseXP() {
        // Test with very low base XP
        when(mockConfig.getBaseXP()).thenReturn(10.0);
        when(mockConfig.getExponent()).thenReturn(1.25);
        
        LevelFormula lowBaseFormula = new LevelFormula(mockConfig);
        
        // Level 1: 10 * (1 ^ 1.25) = 10
        assertEquals("Low base level 1", 10.0, lowBaseFormula.getRequiredExperience(1), 0.001);
        
        // Level 10: 10 * (10 ^ 1.25) = 237.14
        assertEquals("Low base level 10", 237.14, lowBaseFormula.getRequiredExperience(10), 0.01);
    }
    
    @Test
    public void testHighBaseXP() {
        // Test with very high base XP
        when(mockConfig.getBaseXP()).thenReturn(1000.0);
        when(mockConfig.getExponent()).thenReturn(1.25);
        
        LevelFormula highBaseFormula = new LevelFormula(mockConfig);
        
        // Level 1: 1000 * (1 ^ 1.25) = 1000
        assertEquals("High base level 1", 1000.0, highBaseFormula.getRequiredExperience(1), 0.001);
        
        // Level 10: 1000 * (10 ^ 1.25) = 23713.74
        assertEquals("High base level 10", 23713.74, highBaseFormula.getRequiredExperience(10), 0.01);
    }
    
    @Test
    public void testFormulaConsistency() {
        // Test that calling the same level multiple times returns the same result
        double first = defaultFormula.getRequiredExperience(7);
        double second = defaultFormula.getRequiredExperience(7);
        double third = defaultFormula.getRequiredExperience(7);
        
        assertEquals("Multiple calls should return same result", first, second, 0.001);
        assertEquals("Multiple calls should return same result", second, third, 0.001);
    }
    
    @Test
    public void testVeryHighLevels() {
        // Test that formula works with very high levels
        double level100 = defaultFormula.getRequiredExperience(100);
        double level200 = defaultFormula.getRequiredExperience(200);
        
        assertTrue("Level 100 should require positive XP", level100 > 0);
        assertTrue("Level 200 should require positive XP", level200 > 0);
        assertTrue("Level 200 should require more than level 100", level200 > level100);
        assertFalse("Result should not be infinite", Double.isInfinite(level200));
        assertFalse("Result should not be NaN", Double.isNaN(level200));
    }
}
