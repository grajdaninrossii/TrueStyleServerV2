package com.truestyle.controller;

import com.truestyle.entity.stuff.ShopStuff;
import com.truestyle.pojo.MessageResponse;
import com.truestyle.pojo.ShopStuffCVData;
import com.truestyle.service.stuff.StuffService;
import com.truestyle.service.stuff.WardrobeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clothes")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StuffController {

    private final WardrobeService wardrobeService;

    private final StuffService stuffService;

    /** Получить рекомендации после классификации
     *
     * @param stuffData - json с информацией о вещи
     * @return вернет 10 вещей из бд
     */
    @PostMapping("/get/cv")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public List<ShopStuff> getCvStuff(@RequestBody ShopStuffCVData stuffData){
        return stuffService.getStuffMLRRecommendation(stuffData);
    }

    /** Получить одежду для рекоммендаций
     *
     * нужен токен!
     * @return JSON(List<Stuff>)
     */
    @GetMapping("/recommended")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public List<ShopStuff> getStuffRecommended(){
        return stuffService.getStuffByRecommended();
    }

    /** Лайкнуть шмотку
     *
     * @param id_stuff - id шмотки + нужен токен!
     * @return вернет сообщение с положительным результатом, иначе косяк сервака
     */
    @PostMapping("/like")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> likeStuff(@RequestParam Long id_stuff){
        ShopStuff shopsStuff = stuffService.findById(id_stuff);
        wardrobeService.likeStuff(shopsStuff);
        return ResponseEntity.ok(new MessageResponse("Stuff LIKED"));
    }

    /** Удалить шмотку из понравившегося
     *
     * @param id_stuff - id шмотки + нужен токен!
     * @return вернет сообщение с положительным результатом, иначе косяк сервака
     */
    @PostMapping("/dislike")
    @PreAuthorize("hasRole('USER') or HasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> dislikeStuff(@RequestParam Long id_stuff){
        ShopStuff shopsStuff = stuffService.findById(id_stuff);
        wardrobeService.dislikeStuff(shopsStuff);
        return ResponseEntity.ok(new MessageResponse("Stuff NOT LIKED"));
    }
}