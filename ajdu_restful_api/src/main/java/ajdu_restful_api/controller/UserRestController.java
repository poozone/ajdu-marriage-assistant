package ajdu_restful_api.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ajdu_restful_api.model.Package;
import ajdu_restful_api.model.Role;
import ajdu_restful_api.model.Schedule;
import ajdu_restful_api.model.User;
import ajdu_restful_api.service.PackageService;
import ajdu_restful_api.service.RoleService;
import ajdu_restful_api.service.ScheduleService;
import ajdu_restful_api.service.UserService;

@RestController
public class UserRestController {

	@Autowired
	private UserService userService;
	@Autowired
	private RoleService roleService;
	@Autowired
	private PackageService packService;
	@Autowired
	private ScheduleService scheduleService;
	
	
	@GetMapping("/hello")
	public String hello(){
		return "Hello World!";
	}
	
	@RequestMapping(value="/users",method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<List<User>> allUsers(){
		return new ResponseEntity<List<User>>(userService.findAll(), HttpStatus.OK);
	}
	
	@RequestMapping(value="/users", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> addUser(@RequestBody User user) {
		if(userService.findUserByLogin(user.getLogin()) == null) {
			if(user.getRoles() == null) 
				user.setRoles(new ArrayList<Role>());
			user.getRoles().add(roleService.findRole(2));
			
			userService.save(user);
			return new ResponseEntity<User>(user, HttpStatus.CREATED);
		}
		else return new ResponseEntity<User>(HttpStatus.CONFLICT);
	}
	
	@RequestMapping(value="/users/{id}",method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> findOneUser(@PathVariable int id){
		User u = userService.findUser(id);
		if(u != null)
				return new ResponseEntity<User>(u, HttpStatus.OK);
		else return new ResponseEntity<User> (HttpStatus.NOT_FOUND);
	}
	
	
	@RequestMapping(value="/users/{id}", method=RequestMethod.DELETE)
	public ResponseEntity<User> deleteUser(@PathVariable int id) {
		User u = userService.findUser(id);
		if(u != null) {
			userService.delete(id);
			return new ResponseEntity<User>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}", method=RequestMethod.PUT)
	public ResponseEntity<User> updateUser(@PathVariable int id, @RequestBody User user) {
		User u = userService.findUser(id);
		if(u != null) {
			u.setActive(user.isActive());
			u.setEmail(user.getEmail());
			u.setFirstName(user.getFirstName());
			u.setLastName(user.getLastName());
			u.setLogin(user.getLogin());
			u.setPassword(user.getPassword());
			u.setRoles(user.getRoles());
			userService.save(u);
			return new ResponseEntity<User>(u, HttpStatus.OK);
		}
		return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/by/login/{login}",method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<User> findUserByLogin(@PathVariable String login) {
		if(userService.findUserByLogin(login) != null)
			return new ResponseEntity<User> (userService.findUserByLogin(login), HttpStatus.OK);
		else return new ResponseEntity<User> (HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}/schedule", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Schedule> getUserSchedule(@PathVariable int id){
		User u = userService.findUser(id);
		Schedule s = u.getSchedule();
		if(u != null && s != null) 
			return new ResponseEntity<Schedule>(s,HttpStatus.OK);
		else return new ResponseEntity<Schedule>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value="/users/{id}/package", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Package> getUserPackage(@PathVariable int id){
		User u = userService.findUser(id);
		Package p = u.getPack();
		if(u != null && p != null) 
			return new ResponseEntity<Package>(p,HttpStatus.OK);
		else return new ResponseEntity<Package>(HttpStatus.NOT_FOUND);
	}
	

}
