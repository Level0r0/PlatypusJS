/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.model.application.interacting;

import com.bearsoft.rowset.Rowset;
import com.eas.client.DatabasesClient;
import com.eas.client.DatabasesClientWithResource;
import com.eas.client.BaseModelTest;
import com.eas.client.model.application.EntityDataListener;
import com.eas.client.model.application.EntityRefreshFilterDataListener;
import com.eas.client.model.application.ApplicationDbEntity;
import com.eas.client.model.application.ApplicationDbModel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author mg
 */
public class FilteringTest extends BaseModelTest {

    public static String MODEL_TEST_PATH = BaseModelTest.RESOURCES_PREFIX + "datamodelFilteringRelations.xml";
    // 1st layer
    public static Long ENTITY_GRUPPA_OBJECTA_REMONTA_ID = 128049573928131L;
    public static Long ENTITY_VID_OBJECTA_REMONTA_ID = 128049576096827L;
    // 2nd layer
    public static Long ENTITY_IZMERJAEMIE_VELICHINI_ID = 128049576695369L;
    public static Long ENTITY_MARKI_OBJECTOV_REMONTA_ID = 128049574970367L;
    // 3rd layer
    public static Long ENTITY_EDINICI_IZMERENIJA_ID = 128049575554676L;
    public static Long ENTITY_EDINICI_IZMERENIJA_1_ID = 128072901201589L;
    public static Long ENTITY_NAIMENOVANIA_SI_ID = 128049577162567L;
    public static Long ENTITY_EDINICI_OBORUDOVANIJA_ID = 128049574495320L;

    public class ModelState {

        private ApplicationDbModel model = null;
        public ApplicationDbEntity GRUPPA_OBJECTA_REMONTA = null;
        public ApplicationDbEntity VID_OBJECTA_REMONTA = null;
        public ApplicationDbEntity IZMERJAEMIE_VELICHINI = null;
        public ApplicationDbEntity MARKI_OBJECTOV_REMONTA = null;
        public ApplicationDbEntity EDINICI_IZMERENIJA = null;
        public ApplicationDbEntity EDINICI_IZMERENIJA_1 = null;
        public ApplicationDbEntity NAIMENOVANIA_SI = null;
        public ApplicationDbEntity EDINICI_OBORUDOVANIJA = null;

        public ModelState(ApplicationDbModel aModel) throws Exception {
            super();
            model = aModel;
            // 1st layer
            GRUPPA_OBJECTA_REMONTA = model.getEntityById(ENTITY_GRUPPA_OBJECTA_REMONTA_ID);
            assertNotNull(GRUPPA_OBJECTA_REMONTA.getRowset());
            VID_OBJECTA_REMONTA = model.getEntityById(ENTITY_VID_OBJECTA_REMONTA_ID);
            assertNotNull(VID_OBJECTA_REMONTA.getRowset());
            // 2nd layer
            IZMERJAEMIE_VELICHINI = model.getEntityById(ENTITY_IZMERJAEMIE_VELICHINI_ID);
            assertNotNull(IZMERJAEMIE_VELICHINI.getRowset());
            MARKI_OBJECTOV_REMONTA = model.getEntityById(ENTITY_MARKI_OBJECTOV_REMONTA_ID);
            assertNotNull(MARKI_OBJECTOV_REMONTA.getRowset());
            // 3rd layer
            EDINICI_IZMERENIJA = model.getEntityById(ENTITY_EDINICI_IZMERENIJA_ID);
            assertNotNull(EDINICI_IZMERENIJA.getRowset());
            EDINICI_IZMERENIJA_1 = model.getEntityById(ENTITY_EDINICI_IZMERENIJA_1_ID);
            assertNotNull(EDINICI_IZMERENIJA_1.getRowset());
            NAIMENOVANIA_SI = model.getEntityById(ENTITY_NAIMENOVANIA_SI_ID);
            assertNotNull(NAIMENOVANIA_SI.getRowset());
            EDINICI_OBORUDOVANIJA = model.getEntityById(ENTITY_EDINICI_OBORUDOVANIJA_ID);
            assertNotNull(EDINICI_OBORUDOVANIJA.getRowset());
        }

        public Map<Long, Integer> gatherRowCounts() throws Exception {
            Map<Long, Integer> counts = new HashMap<>();
            for (ApplicationDbEntity entity : model.getEntities().values()) {
                counts.put(entity.getEntityId(), entity.getRowset().size());
            }
            return counts;
        }

        public void ensureRowCounts(Map<Long, Integer> counts, Long toSkip) throws Exception {
            for (Long entityID : counts.keySet()) {
                if (!entityID.equals(toSkip)) {
                    assertEquals((Integer) counts.get(entityID), (Integer) model.getEntityById(entityID).getRowset().size());
                }
            }
        }
    }
    public static final Long DLINA = 124772604470311L;
    public static final Long SILA_EL = 124772776092170L;
    public static final Long SILA_LIGHT = 124772784578140L;
    public static final Long DAVL = 128049701701571L;
    public static final Long MOSHN = 128049695192184L;
    public static final Long NAPRJAZH = 128049696157809L;
    public static final Long VOLUME = 124772787892110L;

    @Test
    public void filteringMultiTypesKeysTest() throws Exception {
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            System.out.println("Test of filtering process with key values of various types, but same values");
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();
            ModelState state = new ModelState(model);
            Rowset rowset = state.IZMERJAEMIE_VELICHINI.getRowset();
            int pkColIndex = rowset.getFields().find("ID");

            // BigDecimal test
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(SILA_EL)) {
                        // avoid converting to test pre filter converting capability of our entities
                        rowset.getCurrentRow().setColumnObject(pkColIndex, BigDecimal.valueOf(SILA_EL));
                    }
                }
            }
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(SILA_EL)) {
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    }
                }
            }

            // BigInteger test
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(SILA_EL)) {
                        // avoid converting to test pre filter converting capability of our entities
                        rowset.getCurrentRow().setColumnObject(pkColIndex, BigInteger.valueOf(SILA_EL));
                    }
                }
            }
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(SILA_EL)) {
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    }
                }
            }

            // Integer test
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(SILA_EL)) {
                        // avoid converting to test pre filter converting capability of our entities
                        rowset.getCurrentRow().setColumnObject(pkColIndex, Integer.valueOf(SILA_EL.intValue()));
                    }
                }
            }
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(SILA_EL)) {
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    }
                }
            }
        }
    }

    @Test
    public void userFilteringTest() throws Exception {
        System.out.println("Enable and disable user custom filtering");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();
            ModelState state = new ModelState(model);
            Rowset rowset = state.IZMERJAEMIE_VELICHINI.getRowset();
            int pkColIndex = rowset.getFields().find("ID");
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA)) {
                        assertEquals(0, state.NAIMENOVANIA_SI.getRowset().size());
                    }
                }
            }

            // let's apply user filtering
            state.NAIMENOVANIA_SI.setUserFiltering(true);
            try {
                rowset.beforeFirst();
                while (rowset.next()) {
                    assertEquals(18, state.NAIMENOVANIA_SI.getRowset().size());
                }
            } finally {
                // let's cancel user filtering
                state.NAIMENOVANIA_SI.setUserFiltering(false);
            }

            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA)) {
                        assertEquals(0, state.NAIMENOVANIA_SI.getRowset().size());
                    }
                }
            }
        }
    }

    @Test
    public void filteringScrollTest() throws Exception {
        System.out.println("filteringScrollTest, filteringPointsOfIntererstTest");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();
            ModelState state = new ModelState(model);
            Map<Long, Integer> counts = state.gatherRowCounts();

            // let's move some rowset's la la la la...
            Rowset rowset = state.GRUPPA_OBJECTA_REMONTA.getRowset();
            rowset.beforeFirst();
            while (rowset.next()) {
                state.ensureRowCounts(counts, state.GRUPPA_OBJECTA_REMONTA.getEntityId());
            }
            rowset.first();

            rowset = state.VID_OBJECTA_REMONTA.getRowset();
            rowset.beforeFirst();
            while (rowset.next()) {
                state.ensureRowCounts(counts, state.VID_OBJECTA_REMONTA.getEntityId());
            }
            rowset.first();

            rowset = state.IZMERJAEMIE_VELICHINI.getRowset();
            int pkColIndex = rowset.getFields().find("ID");
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA)) {
                        assertEquals(4, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(0, state.NAIMENOVANIA_SI.getRowset().size());
                        Rowset dlinaRowset = state.EDINICI_IZMERENIJA.getRowset();
                        dlinaRowset.beforeFirst();
                        while (dlinaRowset.next()) {
                            assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        }
                        dlinaRowset.first();
                    } else if (lPk.equals(SILA_EL)) {
                        assertEquals(1, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    } else if (lPk.equals(DAVL)) {
                        assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    } else if (lPk.equals(MOSHN)) {
                        assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    } else if (lPk.equals(NAPRJAZH)) {
                        assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(1, state.NAIMENOVANIA_SI.getRowset().size());
                    } else if (lPk.equals(VOLUME)) {
                        assertEquals(1, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(0, state.NAIMENOVANIA_SI.getRowset().size());
                    } else {
                        assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        assertEquals(0, state.NAIMENOVANIA_SI.getRowset().size());
                    }
                } else {
                    assertTrue(false);
                }
            }
            rowset.first();

            rowset = state.MARKI_OBJECTOV_REMONTA.getRowset();
            pkColIndex = rowset.getFields().find("ID");
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(128049594110963L)) {
                        assertEquals(2, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049595046828L)) {
                        assertEquals(2, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049595600024L)) {
                        assertEquals(2, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049596076572L)) {
                        assertEquals(3, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049596964037L)) {
                        assertEquals(2, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049597468768L)) {
                        assertEquals(2, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049597975084L)) {
                        assertEquals(4, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049598748403L)) {
                        assertEquals(2, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049599817169L)) {
                        assertEquals(0, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    } else if (lPk.equals(128049601170306L)) {
                        assertEquals(1, state.EDINICI_OBORUDOVANIJA.getRowset().size());
                    }
                } else {
                    assertTrue(false);
                }
            }
            rowset.first();
        }
    }

    @Test
    public void filteringEmptyKeysSourceTest() throws Exception {
        System.out.println("filteringEmptyKeysSourceTest");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();
            ModelState state = new ModelState(model);

            Rowset rowset = state.IZMERJAEMIE_VELICHINI.getRowset();
            int pkColIndex = rowset.getFields().find("ID");
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA) || lPk.equals(SILA_EL) || lPk.equals(VOLUME)) {
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                    } else {
                        assertNotNull(state.EDINICI_IZMERENIJA_1.getRowset());
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                    }
                } else {
                    assertTrue(false);
                }
            }
            rowset.first();
        }
    }

    @Test
    public void filteringBadSourcePositionTest() throws Exception {
        System.out.println("filteringBadSourcePositionTest");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();
            ModelState state = new ModelState(model);

            Rowset rowset = state.IZMERJAEMIE_VELICHINI.getRowset();
            int pkColIndex = rowset.getFields().find("ID");
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA) || lPk.equals(SILA_EL) || lPk.equals(VOLUME)) {
                        // ensure state
                        assertNotNull(state.EDINICI_IZMERENIJA_1.getRowset());
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        // before first
                        state.EDINICI_IZMERENIJA.getRowset().beforeFirst();
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        state.EDINICI_IZMERENIJA.getRowset().first();
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        // after last
                        state.EDINICI_IZMERENIJA.getRowset().afterLast();
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                        state.EDINICI_IZMERENIJA.getRowset().last();
                        assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                    } else {
                        assertNotNull(state.EDINICI_IZMERENIJA_1.getRowset());
                        assertEquals(0, state.EDINICI_IZMERENIJA_1.getRowset().size());
                    }
                } else {
                    assertTrue(false);
                }
            }
            rowset.first();
        }
    }

    @Test
    public void filteringCrudTest() throws Exception {
        System.out.println("filteringCrudTest");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();
            ModelState state = new ModelState(model);
            Rowset rowset = state.IZMERJAEMIE_VELICHINI.getRowset();
            int pkColIndex = rowset.getFields().find("ID");

            // edit rows from dline to sila sveta and vice versa for several times
            for (int i = 0; i < 56; i++) {
                // ensure prestate
                rowset.beforeFirst();
                while (rowset.next()) {
                    Object oPk = rowset.getObject(pkColIndex);
                    assertNotNull(oPk);
                    if (oPk instanceof Number) {
                        Long lPk = ((Number) oPk).longValue();
                        if (lPk.equals(DLINA)) {
                            assertEquals(4, state.EDINICI_IZMERENIJA.getRowset().size());
                        } else if (lPk.equals(SILA_LIGHT)) {
                            assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        }
                    }
                }
                // let's edit filter-key field's value.
                rowset.beforeFirst();
                while (rowset.next()) {
                    Object oPk = rowset.getObject(pkColIndex);
                    assertNotNull(oPk);
                    if (oPk instanceof Number) {
                        Long lPk = ((Number) oPk).longValue();
                        if (lPk.equals(DLINA)) {
                            Rowset edRowset = state.EDINICI_IZMERENIJA.getRowset();
                            int velColIndex = edRowset.getFields().find("MEASURAND");
                            int updated = 0;
                            while (edRowset.size() > 0) {
                                edRowset.first();
                                edRowset.updateObject(velColIndex, SILA_LIGHT);
                                updated++;
                                assertTrue(updated <= 4);
                            }
                        }
                    }
                }
                // ensure results
                rowset.beforeFirst();
                while (rowset.next()) {
                    Object oPk = rowset.getObject(pkColIndex);
                    assertNotNull(oPk);
                    if (oPk instanceof Number) {
                        Long lPk = ((Number) oPk).longValue();
                        if (lPk.equals(DLINA)) {
                            assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        } else if (lPk.equals(SILA_LIGHT)) {
                            assertEquals(4, state.EDINICI_IZMERENIJA.getRowset().size());
                        }
                    }
                }
                // let's edit filter-key field's value.
                rowset.beforeFirst();
                while (rowset.next()) {
                    Object oPk = rowset.getObject(pkColIndex);
                    assertNotNull(oPk);
                    if (oPk instanceof Number) {
                        Long lPk = ((Number) oPk).longValue();
                        if (lPk.equals(SILA_LIGHT)) {
                            Rowset edRowset = state.EDINICI_IZMERENIJA.getRowset();
                            int velColIndex = edRowset.getFields().find("MEASURAND");
                            int updated = 0;
                            while (edRowset.size() > 0) {
                                edRowset.first();
                                edRowset.updateObject(velColIndex, DLINA);
                                updated++;
                                assertTrue(updated <= 4);
                            }
                        }
                    }
                }
                // ensure results
                rowset.beforeFirst();
                while (rowset.next()) {
                    Object oPk = rowset.getObject(pkColIndex);
                    assertNotNull(oPk);
                    if (oPk instanceof Number) {
                        Long lPk = ((Number) oPk).longValue();
                        if (lPk.equals(DLINA)) {
                            assertEquals(4, state.EDINICI_IZMERENIJA.getRowset().size());
                        } else if (lPk.equals(SILA_LIGHT)) {
                            assertEquals(0, state.EDINICI_IZMERENIJA.getRowset().size());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void filteringScriptEventsVsDataEventsTest() throws Exception {
        System.out.println("filteringScriptEventsVsDataTest");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
            model.requery();

            ModelState state = new ModelState(model);

            EntityDataListener dataListener = new EntityDataListener();
            state.EDINICI_IZMERENIJA.getRowset().addRowsetListener(dataListener);

            EntityDataListener dataListener1 = new EntityDataListener();
            state.EDINICI_IZMERENIJA_1.getRowset().addRowsetListener(dataListener1);

            EntityDataListener dataListener2 = new EntityDataListener();
            state.NAIMENOVANIA_SI.getRowset().addRowsetListener(dataListener2);

            Rowset rowset = state.IZMERJAEMIE_VELICHINI.getRowset();

            int pkColIndex = rowset.getFields().find("ID");
            dataListener.reset();
            dataListener1.reset();
            dataListener2.reset();
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA)) {
                        Rowset edRowset = state.EDINICI_IZMERENIJA.getRowset();
                        assertEquals(4, edRowset.size());
                    }
                }
            }
            rowset.first();
///////////////////////////////////////////////

            state.EDINICI_IZMERENIJA.getRowset().removeRowsetListener(dataListener);

            dataListener1.reset();
            dataListener2.reset();
            rowset.beforeFirst();
            while (rowset.next()) {
                Object oPk = rowset.getObject(pkColIndex);
                assertNotNull(oPk);
                if (oPk instanceof Number) {
                    Long lPk = ((Number) oPk).longValue();
                    if (lPk.equals(DLINA)) {
                        Rowset edRowset = state.EDINICI_IZMERENIJA.getRowset();
                        assertEquals(4, edRowset.size());
                        edRowset.beforeFirst();
                        while (edRowset.next()) {
                            assertEquals(1, state.EDINICI_IZMERENIJA_1.getRowset().size());
                            assertEquals(0, state.NAIMENOVANIA_SI.getRowset().size());
                        }
                        edRowset.first();
                    }
                }
            }
            rowset.first();
        }
    }
    EntityRefreshFilterDataListener listenerOf1Layer = null;
    EntityRefreshFilterDataListener listenerOf2Layer = null;
    EntityRefreshFilterDataListener listenerOf3Layer = null;

    @Test
    public void filteringExecutingOrderTest() throws Exception {
        System.out.println("filteringExecutingOrderTest layer by layer");
        try (DatabasesClientWithResource resource = BaseModelTest.initDevelopTestClient()) {
            final DatabasesClient client = resource.getClient();
            try {
                ApplicationDbModel model = BaseModelTest.modelFromStream(client, FilteringTest.class.getResourceAsStream(MODEL_TEST_PATH));
                model.requery();

                ModelState state = new ModelState(model);

                listenerOf1Layer = null;
                listenerOf2Layer = null;
                listenerOf3Layer = null;

                listenerOf1Layer = new EntityRefreshFilterDataListener() {
                    @Override
                    protected void incEvents() {
                        super.incEvents();
                        assertEquals(0, listenerOf2Layer.getEvents());
                        assertEquals(0, listenerOf3Layer.getEvents());
                    }
                };
                listenerOf2Layer = new EntityRefreshFilterDataListener() {
                    @Override
                    protected void incEvents() {
                        super.incEvents();
                        assertTrue(listenerOf1Layer.getEvents() > 0);
                        assertEquals(0, listenerOf3Layer.getEvents());
                    }
                };
                listenerOf3Layer = new EntityRefreshFilterDataListener() {
                    @Override
                    protected void incEvents() {
                        super.incEvents();
                        assertTrue(listenerOf1Layer.getEvents() > 0);
                        assertTrue(listenerOf2Layer.getEvents() > 0);
                    }
                };
                // 1st layer
                state.GRUPPA_OBJECTA_REMONTA.getRowset().addRowsetListener(listenerOf1Layer);
                state.VID_OBJECTA_REMONTA.getRowset().addRowsetListener(listenerOf1Layer);
                state.IZMERJAEMIE_VELICHINI.getRowset().addRowsetListener(listenerOf1Layer);
                state.MARKI_OBJECTOV_REMONTA.getRowset().addRowsetListener(listenerOf1Layer);
                // 2nd layer
                state.EDINICI_IZMERENIJA.getRowset().addRowsetListener(listenerOf2Layer);
                state.NAIMENOVANIA_SI.getRowset().addRowsetListener(listenerOf2Layer);
                state.EDINICI_OBORUDOVANIJA.getRowset().addRowsetListener(listenerOf2Layer);
                // 3rd layer
                state.EDINICI_IZMERENIJA_1.getRowset().addRowsetListener(listenerOf3Layer);

                model.requery();
            } finally {
                listenerOf1Layer = null;
                listenerOf2Layer = null;
                listenerOf3Layer = null;
            }
        }
    }
}