package ajdu_restful_api.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ajdu_restful_api.model.Package;
import ajdu_restful_api.model.Role;
import ajdu_restful_api.model.Schedule;
import ajdu_restful_api.model.TodoTask;
import ajdu_restful_api.model.User;
import ajdu_restful_api.service.PackageService;
import ajdu_restful_api.service.PartnerService;
import ajdu_restful_api.service.TodoTaskService;
import ajdu_restful_api.service.UserService;

@RestController
public class UserRestController extends AuthenticatedRestController {

	@Autowired
	private UserService userService;
	@Autowired
	private PartnerService partnerService;
	@Autowired
	private PackageService packService;
	@Autowired
	private TodoTaskService todoService;
	
	@GetMapping("/hello")
	public String hello(){
		return "Hello World!";
	}
	
	@RequestMapping(value="/users",method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<User>> allUsers(){
		return new ResponseEntity<List<User>>(userService.findAll(), HttpStatus.OK);
	}
	
	@RequestMapping(value="/users", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> addUser(@RequestBody User user, Authentication auth) {
			if(user.getLogin() != null &&
					user.getGender() != null &&
					user.getFirstName() != null && 
					user.getLastName() != null && 
					user.getEmail() != null && 
					user.getPassword() != null
					) {
				if(userService.findUserByLogin(user.getLogin()) == null) {
					
					if(user.getRoles() == null || user.getRoles().isEmpty()) {
						List<Role> roles = new ArrayList<Role>();
						roles.add(Role.REG_USER);
						user.setRoles(roles);
					}
					userService.save(user);	
				
					// adding schedule for user on creation
					Schedule schedule = new Schedule();
					schedule.setUser(user);
					user.setSchedule(schedule);					
				
					// adding package for user on creation
					Package pack = new Package();
					pack.setTotalCost(BigDecimal.ZERO);
					pack.setUser(user);
					packService.save(pack);
					user.setPack(pack);	
					
					List<TodoTask> todosFromDB = todoService.findAllTasksByUserDefined(false);
					List<TodoTask> usersTodos = new ArrayList<TodoTask>();
					for(TodoTask t: todosFromDB) {
						TodoTask todo = new TodoTask(t.getTitle(), t.getDescription(), true);
						todo.setUser(user);
						todoService.saveTask(todo);
						usersTodos.add(todo);
					}
					user.setTodoTasks(usersTodos);
					
					userService.save(user);
					return new ResponseEntity<User>(user, HttpStatus.CREATED);
				}
				else return new ResponseEntity<User>(HttpStatus.CONFLICT);
			} else return new ResponseEntity<User>(HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value="/users/{userId}/permitted-users", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> addPermittedUser(@PathVariable int userId, @RequestBody User user, Authentication auth) {
		User u = userService.findUser(userId);
		if(u != null && user.getId() != null){
			if(hasPermission(auth, u.getLogin())) {
				User permUser = userService.findUser(user.getId());
				if(u.getId() == null || permUser.getId() == null || permUser.getId() == u.getId())
					return new ResponseEntity<User>(HttpStatus.BAD_REQUEST);
				
				for(User us : u.getPermittedUsers()) {
					if(us.getId() == permUser.getId())
						return new ResponseEntity<User>(HttpStatus.CONFLICT);
				}
				
				permUser.getMainUsers().add(u);
				userService.save(permUser);
				return new ResponseEntity<User>(user, HttpStatus.OK);
			} else return new ResponseEntity<User>(HttpStatus.FORBIDDEN);
		}
		else return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{userId}/permitted-users", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<User>> getPermittedUser(@PathVariable int userId, Authentication auth) {
		User u = userService.findUser(userId);		
			if(u!=null && u.getPermittedUsers()!=null && !u.getPermittedUsers().isEmpty()) {
				if(hasPermission(auth, u.getLogin()))
					return new ResponseEntity<List<User>>(u.getPermittedUsers(), HttpStatus.OK);
				else return new ResponseEntity<List<User>>(HttpStatus.FORBIDDEN);	
			}
			else return new ResponseEntity<List<User>>(HttpStatus.NOT_FOUND); 
	}
	
	
	
	@RequestMapping(value="/users/{id}",method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> findOneUser(@PathVariable int id, Authentication auth){
		User u = userService.findUser(id);
		if(u != null)
			if(hasPermission(auth, u.getLogin()))
				return new ResponseEntity<User>(u, HttpStatus.OK);
			else return new ResponseEntity<User>(HttpStatus.FORBIDDEN);		
		else return new ResponseEntity<User> (HttpStatus.NOT_FOUND);
	}
	
	
	@RequestMapping(value="/users/{id}", method=RequestMethod.DELETE)
	public ResponseEntity<User> deleteUser(@PathVariable int id, Authentication auth) {
		User u = userService.findUser(id);
		if(u != null) {
			if(hasPermission(auth, u.getLogin())) {
				userService.delete(id);
				return new ResponseEntity<User>(HttpStatus.NO_CONTENT);				
			} else return new ResponseEntity<User>(HttpStatus.FORBIDDEN);
		}
		return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}", method=RequestMethod.PUT)
	public ResponseEntity<User> updateUser(@PathVariable int id, @RequestBody User user, Authentication auth) {
		User u = userService.findUser(id);
		if(u != null) {
			if(hasPermission(auth, u.getLogin())) {
				user.setId(id);
				if(u.getPartner() != null)
					user.getPartner().setId(u.getPartner().getId());
				partnerService.save(user.getPartner());
				userService.save(user);
				return new ResponseEntity<User>(user, HttpStatus.OK);
			} else return new ResponseEntity<User>(HttpStatus.FORBIDDEN);
		}
		return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}",method=RequestMethod.PATCH, consumes=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> patchService(@PathVariable int id, @RequestBody User user, Authentication auth) {
		User u = userService.findUser(id);
		if(u != null) {
			if(!hasPermission(auth, u.getLogin()))
				return new ResponseEntity<User>(HttpStatus.FORBIDDEN);
			
			userService.savePartial(user, id);
			return new ResponseEntity<User>(user, HttpStatus.OK);
		}
		else return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/by/login",method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> findUserByLogin(@RequestParam String login, Authentication auth) {
		if(userService.findUserByLogin(login) != null)
			if(hasPermission(auth, login))
				return new ResponseEntity<User> (userService.findUserByLogin(login), HttpStatus.OK);
			else return new ResponseEntity<User>(HttpStatus.FORBIDDEN);
		else return new ResponseEntity<User> (HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}/schedule", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Schedule> getUserSchedule(@PathVariable int id, Authentication auth){
		User u = userService.findUser(id);
		Schedule s = u.getSchedule();
		if(u != null && s != null) 
			if(hasPermission(auth, u.getLogin()))
				return new ResponseEntity<Schedule>(s,HttpStatus.OK);
			else return new ResponseEntity<Schedule>(HttpStatus.FORBIDDEN);
		else return new ResponseEntity<Schedule>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}/todos", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<TodoTask>> getUserTodos(@PathVariable int id, Authentication auth){
		User u = userService.findUser(id);
		if(u != null && u.getTodoTasks() != null) 
			if(hasPermission(auth, u.getLogin()))
				return new ResponseEntity<List<TodoTask>>(u.getTodoTasks(),HttpStatus.OK);
			else return new ResponseEntity<List<TodoTask>>(HttpStatus.FORBIDDEN);
		else return new ResponseEntity<List<TodoTask>>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}/package", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Package> getUserPackage(@PathVariable int id, Authentication auth){
		User u = userService.findUser(id);
		if(u != null) {
			Package p = u.getPack();
			if(p != null) {
				if(hasPermission(auth, u.getLogin()))
					return new ResponseEntity<Package>(p,HttpStatus.OK);				
				else return new ResponseEntity<Package>(HttpStatus.FORBIDDEN);
			} else return new ResponseEntity<Package>(HttpStatus.NOT_FOUND);
		} else return new ResponseEntity<Package>(HttpStatus.NOT_FOUND);
	}

}
