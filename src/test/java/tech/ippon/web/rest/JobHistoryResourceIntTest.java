package tech.ippon.web.rest;

import tech.ippon.JhipsterApp;

import tech.ippon.domain.JobHistory;
import tech.ippon.repository.JobHistoryRepository;
import tech.ippon.repository.search.JobHistorySearchRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the JobHistoryResource REST controller.
 *
 * @see JobHistoryResource
 */
@RunWith(SpringRunner.class)

@SpringBootTest(classes = JhipsterApp.class)

public class JobHistoryResourceIntTest {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("Z"));

    private static final ZonedDateTime DEFAULT_START_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_START_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final String DEFAULT_START_DATE_STR = dateTimeFormatter.format(DEFAULT_START_DATE);

    private static final ZonedDateTime DEFAULT_END_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_END_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final String DEFAULT_END_DATE_STR = dateTimeFormatter.format(DEFAULT_END_DATE);

    @Inject
    private JobHistoryRepository jobHistoryRepository;

    @Inject
    private JobHistorySearchRepository jobHistorySearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restJobHistoryMockMvc;

    private JobHistory jobHistory;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        JobHistoryResource jobHistoryResource = new JobHistoryResource();
        ReflectionTestUtils.setField(jobHistoryResource, "jobHistorySearchRepository", jobHistorySearchRepository);
        ReflectionTestUtils.setField(jobHistoryResource, "jobHistoryRepository", jobHistoryRepository);
        this.restJobHistoryMockMvc = MockMvcBuilders.standaloneSetup(jobHistoryResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JobHistory createEntity(EntityManager em) {
        JobHistory jobHistory = new JobHistory()
                .startDate(DEFAULT_START_DATE)
                .endDate(DEFAULT_END_DATE);
        return jobHistory;
    }

    @Before
    public void initTest() {
        jobHistorySearchRepository.deleteAll();
        jobHistory = createEntity(em);
    }

    @Test
    @Transactional
    public void createJobHistory() throws Exception {
        int databaseSizeBeforeCreate = jobHistoryRepository.findAll().size();

        // Create the JobHistory

        restJobHistoryMockMvc.perform(post("/api/job-histories")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(jobHistory)))
                .andExpect(status().isCreated());

        // Validate the JobHistory in the database
        List<JobHistory> jobHistories = jobHistoryRepository.findAll();
        assertThat(jobHistories).hasSize(databaseSizeBeforeCreate + 1);
        JobHistory testJobHistory = jobHistories.get(jobHistories.size() - 1);
        assertThat(testJobHistory.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testJobHistory.getEndDate()).isEqualTo(DEFAULT_END_DATE);

        // Validate the JobHistory in ElasticSearch
        JobHistory jobHistoryEs = jobHistorySearchRepository.findOne(testJobHistory.getId());
        assertThat(jobHistoryEs).isEqualToComparingFieldByField(testJobHistory);
    }

    @Test
    @Transactional
    public void getAllJobHistories() throws Exception {
        // Initialize the database
        jobHistoryRepository.saveAndFlush(jobHistory);

        // Get all the jobHistories
        restJobHistoryMockMvc.perform(get("/api/job-histories?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(jobHistory.getId().intValue())))
                .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE_STR)))
                .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE_STR)));
    }

    @Test
    @Transactional
    public void getJobHistory() throws Exception {
        // Initialize the database
        jobHistoryRepository.saveAndFlush(jobHistory);

        // Get the jobHistory
        restJobHistoryMockMvc.perform(get("/api/job-histories/{id}", jobHistory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(jobHistory.getId().intValue()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE_STR))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE_STR));
    }

    @Test
    @Transactional
    public void getNonExistingJobHistory() throws Exception {
        // Get the jobHistory
        restJobHistoryMockMvc.perform(get("/api/job-histories/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateJobHistory() throws Exception {
        // Initialize the database
        jobHistoryRepository.saveAndFlush(jobHistory);
        jobHistorySearchRepository.save(jobHistory);
        int databaseSizeBeforeUpdate = jobHistoryRepository.findAll().size();

        // Update the jobHistory
        JobHistory updatedJobHistory = jobHistoryRepository.findOne(jobHistory.getId());
        updatedJobHistory
                .startDate(UPDATED_START_DATE)
                .endDate(UPDATED_END_DATE);

        restJobHistoryMockMvc.perform(put("/api/job-histories")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedJobHistory)))
                .andExpect(status().isOk());

        // Validate the JobHistory in the database
        List<JobHistory> jobHistories = jobHistoryRepository.findAll();
        assertThat(jobHistories).hasSize(databaseSizeBeforeUpdate);
        JobHistory testJobHistory = jobHistories.get(jobHistories.size() - 1);
        assertThat(testJobHistory.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testJobHistory.getEndDate()).isEqualTo(UPDATED_END_DATE);

        // Validate the JobHistory in ElasticSearch
        JobHistory jobHistoryEs = jobHistorySearchRepository.findOne(testJobHistory.getId());
        assertThat(jobHistoryEs).isEqualToComparingFieldByField(testJobHistory);
    }

    @Test
    @Transactional
    public void deleteJobHistory() throws Exception {
        // Initialize the database
        jobHistoryRepository.saveAndFlush(jobHistory);
        jobHistorySearchRepository.save(jobHistory);
        int databaseSizeBeforeDelete = jobHistoryRepository.findAll().size();

        // Get the jobHistory
        restJobHistoryMockMvc.perform(delete("/api/job-histories/{id}", jobHistory.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean jobHistoryExistsInEs = jobHistorySearchRepository.exists(jobHistory.getId());
        assertThat(jobHistoryExistsInEs).isFalse();

        // Validate the database is empty
        List<JobHistory> jobHistories = jobHistoryRepository.findAll();
        assertThat(jobHistories).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchJobHistory() throws Exception {
        // Initialize the database
        jobHistoryRepository.saveAndFlush(jobHistory);
        jobHistorySearchRepository.save(jobHistory);

        // Search the jobHistory
        restJobHistoryMockMvc.perform(get("/api/_search/job-histories?query=id:" + jobHistory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(jobHistory.getId().intValue())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE_STR)))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE_STR)));
    }
}
