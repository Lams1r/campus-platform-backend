package com.campus.system.modules.edu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.system.modules.edu.entity.EduDropRequest;
import com.campus.system.modules.edu.mapper.EduDropRequestMapper;
import com.campus.system.modules.edu.service.IEduDropRequestService;
import org.springframework.stereotype.Service;

@Service
public class EduDropRequestServiceImpl extends ServiceImpl<EduDropRequestMapper, EduDropRequest> implements IEduDropRequestService {
}
