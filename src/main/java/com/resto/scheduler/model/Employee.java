package com.resto.scheduler.model;
import jakarta.persistence.*; import jakarta.validation.constraints.*;
@Entity public class Employee {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @NotBlank private String fullName; @Email @Column(unique=true) private String email;
  private boolean active = true;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getFullName(){return fullName;} public void setFullName(String n){this.fullName=n;}
  public String getEmail(){return email;} public void setEmail(String e){this.email=e;}
  public boolean isActive(){return active;} public void setActive(boolean a){this.active=a;}
}
