package tech.ippon.web.rest;

import tech.ippon.JhipsterApp;

import tech.ippon.domain.Job;
import tech.ippon.repository.JobRepository;
import tech.ippon.repository.search.JobSearchRepository;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the JobResource REST controller.
 *
 * @see JobResource
 */
@RunWith(SpringRunner.class)

@SpringBootTest(classes = JhipsterApp.class)

public class JobResourceIntTest {

    private static final Long DEFAULT_JOB_ID = 1L;
    private static final Long UPDATED_JOB_ID = 2L;
    private static final String DEFAULT_JOB_TITLE = "AAAAA";
    private static final String UPDATED_JOB_TITLE = "BBBBB";

    private static final Long DEFAULT_MIN_SALARY = 1L;
    private static final Long UPDATED_MIN_SALARY = 2L;

    private static final Long DEFAULT_MAX_SALARY = 1L;
    private static final Long UPDATED_MAX_SALARY = 2L;

    @Inject
    private JobRepository jobRepository;

    @Inject
    private JobSearchRepository jobSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restJobMockMvc;

    private Job job;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        JobResource jobResource = new JobResource();
        ReflectionTestUtils.setField(jobResource, "jobSearchRepository", jobSearchRepository);
        ReflectionTestUtils.setField(jobResource, "jobRepository", jobRepository);
        this.restJobMockMvc = MockMvcBuilders.standaloneSetup(jobResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Job createEntity(EntityManager em) {
        Job job = new Job()
                .jobId(DEFAULT_JOB_ID)
                .jobTitle(DEFAULT_JOB_TITLE)
                .minSalary(DEFAULT_MIN_SALARY)
                .maxSalary(DEFAULT_MAX_SALARY);
        return job;
    }

    @Before
    public void initTest() {
        jobSearchRepository.deleteAll();
        job = createEntity(em);
    }

    @Test
    @Transactional
    public void createJob() throws Exception {
        int databaseSizeBeforeCreate = jobRepository.findAll().size();

        // Create the Job

        restJobMockMvc.perform(post("/api/jobs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(job)))
                .andExpect(status().isCreated());

        // Validate the Job in the database
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(databaseSizeBeforeCreate + 1);
        Job testJob = jobs.get(jobs.size() - 1);
        assertThat(testJob.getJobId()).isEqualTo(DEFAULT_JOB_ID);
        assertThat(testJob.getJobTitle()).isEqualTo(DEFAULT_JOB_TITLE);
        assertThat(testJob.getMinSalary()).isEqualTo(DEFAULT_MIN_SALARY);
        assertThat(testJob.getMaxSalary()).isEqualTo(DEFAULT_MAX_SALARY);

        // Validate the Job in ElasticSearch
        Job jobEs = jobSearchRepository.findOne(testJob.getId());
        assertThat(jobEs).isEqualToComparingFieldByField(testJob);
    }

    @Test
    @Transactional
    public void getAllJobs() throws Exception {
        // Initialize the database
        jobRepository.saveAndFlush(job);

        // Get all the jobs
        restJobMockMvc.perform(get("/api/jobs?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(job.getId().intValue())))
                .andExpect(jsonPath("$.[*].jobId").value(hasItem(DEFAULT_JOB_ID.intValue())))
                .andExpect(jsonPath("$.[*].jobTitle").value(hasItem(DEFAULT_JOB_TITLE.toString())))
                .andExpect(jsonPath("$.[*].minSalary").value(hasItem(DEFAULT_MIN_SALARY.intValue())))
                .andExpect(jsonPath("$.[*].maxSalary").value(hasItem(DEFAULT_MAX_SALARY.intValue())));
    }

    @Test
    @Transactional
    public void getJob() throws Exception {
        // Initialize the database
        jobRepository.saveAndFlush(job);

        // Get the job
        restJobMockMvc.perform(get("/api/jobs/{id}", job.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(job.getId().intValue()))
            .andExpect(jsonPath("$.jobId").value(DEFAULT_JOB_ID.intValue()))
            .andExpect(jsonPath("$.jobTitle").value(DEFAULT_JOB_TITLE.toString()))
            .andExpect(jsonPath("$.minSalary").value(DEFAULT_MIN_SALARY.intValue()))
            .andExpect(jsonPath("$.maxSalary").value(DEFAULT_MAX_SALARY.intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingJob() throws Exception {
        // Get the job
        restJobMockMvc.perform(get("/api/jobs/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateJob() throws Exception {
        // Initialize the database
        jobRepository.saveAndFlush(job);
        jobSearchRepository.save(job);
        int databaseSizeBeforeUpdate = jobRepository.findAll().size();

        // Update the job
        Job updatedJob = jobRepository.findOne(job.getId());
        updatedJob
                .jobId(UPDATED_JOB_ID)
                .jobTitle(UPDATED_JOB_TITLE)
                .minSalary(UPDATED_MIN_SALARY)
                .maxSalary(UPDATED_MAX_SALARY);

        restJobMockMvc.perform(put("/api/jobs")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedJob)))
                .andExpect(status().isOk());

        // Validate the Job in the database
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(databaseSizeBeforeUpdate);
        Job testJob = jobs.get(jobs.size() - 1);
        assertThat(testJob.getJobId()).isEqualTo(UPDATED_JOB_ID);
        assertThat(testJob.getJobTitle()).isEqualTo(UPDATED_JOB_TITLE);
        assertThat(testJob.getMinSalary()).isEqualTo(UPDATED_MIN_SALARY);
        assertThat(testJob.getMaxSalary()).isEqualTo(UPDATED_MAX_SALARY);

        // Validate the Job in ElasticSearch
        Job jobEs = jobSearchRepository.findOne(testJob.getId());
        assertThat(jobEs).isEqualToComparingFieldByField(testJob);
    }

    @Test
    @Transactional
    public void deleteJob() throws Exception {
        // Initialize the database
        jobRepository.saveAndFlush(job);
        jobSearchRepository.save(job);
        int databaseSizeBeforeDelete = jobRepository.findAll().size();

        // Get the job
        restJobMockMvc.perform(delete("/api/jobs/{id}", job.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean jobExistsInEs = jobSearchRepository.exists(job.getId());
        assertThat(jobExistsInEs).isFalse();

        // Validate the database is empty
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchJob() throws Exception {
        // Initialize the database
        jobRepository.saveAndFlush(job);
        jobSearchRepository.save(job);

        // Search the job
        restJobMockMvc.perform(get("/api/_search/jobs?query=id:" + job.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(job.getId().intValue())))
            .andExpect(jsonPath("$.[*].jobId").value(hasItem(DEFAULT_JOB_ID.intValue())))
            .andExpect(jsonPath("$.[*].jobTitle").value(hasItem(DEFAULT_JOB_TITLE.toString())))
            .andExpect(jsonPath("$.[*].minSalary").value(hasItem(DEFAULT_MIN_SALARY.intValue())))
            .andExpect(jsonPath("$.[*].maxSalary").value(hasItem(DEFAULT_MAX_SALARY.intValue())));
    }
}
