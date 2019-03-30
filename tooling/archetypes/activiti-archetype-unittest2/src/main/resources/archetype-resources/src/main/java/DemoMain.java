package ${package};

import com.google.common.collect.Maps;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * created by wsj on 2019/3/3
 */
public class DemoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoMain.class);
    public static void main(String[] args) throws ParseException {

        LOGGER.info("流程开始");
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        System.out.println("name==>" + name);
        System.out.println("version==>" + version);
        LOGGER.info("流程引擎名称{},版本{}", name, version);


        //部署流程定义
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("Myprocess1.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        LOGGER.info("部署流程key{},id{},name{}", deployment.getKey(), deploymentId, deployment.getName());

        //查询流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        System.out.println(processDefinition);


        LOGGER.info("流程定义id-->{}",processDefinition.getId());
        LOGGER.info("流程定义name-->{}",processDefinition.getName());

        LOGGER.info("流程定义文件-->{},流程定义key-->{},resource-->{}", processDefinition.getName(), processDefinition.getKey(), processDefinition.getResourceName());

        //启动运行流程
        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程-->{}",processInstance.getProcessDefinitionKey());
        LOGGER.info("流程实例的id-->{}",processInstance.getProcessDefinitionId());

        processTask(processEngine, processInstance);
        LOGGER.info("流程结束");

    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        //处理流程任务
        while (processInstance != null && !processInstance.isEnded()) {

            List<Task> list = processEngine.getTaskService().createTaskQuery().list();
            LOGGER.info("代办处理任务数量{}", list.size());
            for (Task task : list) {
                LOGGER.info("待办任务{}", task.getName());
                //获取表单
                HashMap<String, Object> variables = getMapVariables(processEngine, scanner, task);
                //提交任务
                processEngine.getTaskService().complete(task.getId(),variables);
            }


            //更新流程实例状态
            processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        }
        scanner.close();
    }

    private static HashMap<String, Object> getMapVariables(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        TaskFormData taskFormData = processEngine.getFormService().getTaskFormData(task.getId());
        //获取门店
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        HashMap<String, Object> variables = Maps.newHashMap();
        for (FormProperty formProperty : formProperties) {
            String line = null;
            if (StringFormType.class.isInstance(formProperty.getType())) {
                LOGGER.info("请输入-->{}", formProperty.getName());
                line = scanner.nextLine();
                variables.put(formProperty.getId(),line);
                LOGGER.info("您输入的是-->{}", line);
            } else if (DateFormType.class.isInstance(formProperty.getType())) {
                LOGGER.info("请输入-->{}?格式为(yyyy-MM-dd)", formProperty.getName());
                line = scanner.nextLine();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(line);
                variables.put(formProperty.getId(), date);
                LOGGER.info("您输入的是-->{}", line);
            } else {
                LOGGER.info("类型暂不支持-->{}",formProperty.getType());
            }
        }
        return variables;
    }
}
